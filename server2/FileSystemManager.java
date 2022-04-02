import java.util.ArrayList;

public class FileSystemManager {

    // NOTE: allow concurrent file reading but not if that file is being written on
    // be someone else
    
    private ArrayList<FileAccess> accessList;

    public FileSystemManager() {
        accessList = new ArrayList<FileAccess>();
    }

    private synchronized FileAccess accessFile(String filename) {
        for (FileAccess f : accessList) {
            if (f.getFilename().equals(filename))
                return f;
        }

        FileAccess f = new FileAccess(filename);
        accessList.add(f);
        return f;
    }

    public void writeFileStart(String filename) {
        FileAccess f = accessFile(filename);
        synchronized (f) {
            while (f.getReaders() > 0 || f.getWriters() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            f.addWriter();
        }
    }

    public void writeFileEnd(String filename) {
        FileAccess f = accessFile(filename);
        synchronized (f) {
            f.removeWriter();
            this.notifyAll();
        }
    }

    public void readFileStart(String filename) {
        FileAccess f = accessFile(filename);
        synchronized (f) {
            while (f.getWriters() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            f.addReader();
        }
    }

    public void readFileEnd(String filename) {
        FileAccess f = accessFile(filename);
        synchronized (f) {
            f.removeReader();
            this.notifyAll();
        }
    }
}
