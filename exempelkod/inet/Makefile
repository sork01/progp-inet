DIRNAME = $(shell pwd)

all:
	javac *.java

clean:
	rm -f *.class

server:
	gnome-terminal --working-directory "$(DIRNAME)" -e "bash -c 'java -classpath . ATMServer'"

client:
	gnome-terminal --working-directory "$(DIRNAME)" -e "bash -c 'java -classpath . ATMClient 127.0.0.1'"
