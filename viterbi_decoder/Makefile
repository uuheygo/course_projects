# NOTE: need to run setup jdk first, and run gmake

DIR = .
SRCS = $(wildcard $(DIR)/*.java)
OBJS = $(SRCS:.java=.class)

all:	$(OBJS)

P2 : P2.java 
	Javac P2.java 

clean: FORCE
	rm -f *.class core *~

.SUFFIXES: .java .class

.java.class:
	javac $<

FORCE:
