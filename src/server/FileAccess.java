public class FileAccess {
    
    private String filename;
    private int readers;
    private int writers;

    public FileAccess(String filename) {
        this.filename = filename;
        this.readers = 0;
        this.writers = 0;
    }

    public String getFilename() {
        return filename;
    }

    public int getReaders() {
        return readers;
    }

    public int getWriters() {
        return writers;
    }

    public void addReader() {
        readers++;
    }

    public void removeReader() {
        readers--;
    }

    public void addWriter() {
        writers++;
    }

    public void removeWriter() {
        writers--;
    }
}
