DIRNAME = $(shell pwd)
CLASSPATH = -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar

all:
	rm -f *.class
	javac $(CLASSPATH) *.java

clean:
	rm -f *.class

server:
	gnome-terminal --working-directory "$(DIRNAME)" -e "bash -c 'java $(CLASSPATH) ATMServer 8088'"
client:
	gnome-terminal --working-directory "$(DIRNAME)" -e "bash -c 'java $(CLASSPATH) ATMClient 127.0.0.1 8088'"

client-here:
	java $(CLASSPATH) ATMClient 127.0.0.1 8088

both:
	make; make server; sleep 1s; make client
