timeout -v 10  mvn yamcs:run > output.txt
grep Gracefully output.txt