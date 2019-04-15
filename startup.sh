#!/bin/bash
java -jar target/chordHashing-1.0-SNAPSHOT-allinone.jar -ip 127.0.0.1 -l 2552 1> /tmp/2552.log &
sleep 3
for i in {2553..2559}
do 
	java -jar target/chordHashing-1.0-SNAPSHOT-allinone.jar -ip 127.0.0.1 -l $i -sip 127.0.0.1 -sp 2552 1>/tmp/$i.log &
       	sleep 5
done
