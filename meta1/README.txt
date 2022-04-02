HOW TO RUN

-------------------------------------------

IMPORTANT 1 - Before running the applications

	a. To run server application you must have the files "users.txt" and "addresses.txt" available in the same directory as the ucDrive.jar (2 copies already provided);
	b. A valid "users.txt" file is as follows: <username>,<password>,<directory>;
	c. A valid directory always has <clients/<username>/home>, following every line with the same configuration.
		Example of a "users.txt" file (minus the "---"):
			---
			squalexy,aaa,clients/squalexy/home/imagens
			squalexy2,bbb,clients/squalexy2/home
			---
	d. Every directory in the "users.txt" file must exist in <ucDrive.jar directory>, i.e, every <clients/<username>/home> directory from "users.txt" must exist;
	e. You can create any directory or put any file inside the <source client directory> or <source server directory/clients/<username>/home> directories;

-------------------------------------------

IMPORTANT 2 - Before running the two server applications

Each application must have a different "addresses.txt" file. A file example is as follows (minus the "---"):
	---
	otherServerAddress,localhost
	thisServerPort,7000
	heartbPort,7001
	fileSyncPortReceiver,7002
	fileSyncPortSender,7003
	---

For example, if you want to run this configuration on server1, you must run this configuration on server2:
	---
	otherServerAddress,localhost
	thisServerPort,6000
	heartbPort,7001
	fileSyncPortReceiver,7003
	fileSyncPortSender,7002
	---
You can specify any port you want for "thisServerPort", but fileSyncPortReceiver from server1 must be the same as fileSyncPortSender from server2 and vice versa. You must also know the other server address, in this case we're using "localhost" for both.

-------------------------------------------

1. Run the server application
	a. Open cmd and do: 
		> java -cp ucDrive.jar Server
	
-------------------------------------------

2. Run the client application
	a. Open cmd and do: 
		> java -cp terminal.jar Client

