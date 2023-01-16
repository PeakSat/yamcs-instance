mvn yamcs:run &
PID=$!
sleep 60
if ps -p $PID > /dev/null
then
    echo "Yamcs runs fine with PID = $PID"
    kill $PID
    exit 0
else 
    echo "Yamcs did not start"
    exit 1
fi