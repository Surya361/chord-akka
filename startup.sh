#!/bin/bash
mvn clean package
java -jar target/chordHashing-1.0-SNAPSHOT-allinone.jar -ip 127.0.0.1 -l 3552 1> /tmp/3552.log &
sleep 3
for i in {3553..3559}
do 
	java -jar target/chordHashing-1.0-SNAPSHOT-allinone.jar -ip 127.0.0.1 -l $i -sip 127.0.0.1 -sp 3552 1>/tmp/$i.log &
       	sleep 5
done
