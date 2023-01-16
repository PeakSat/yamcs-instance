mvn yamcs:run > output.txt &
sleep 10
if grep Finished output.txt; then
    exit 1
else
    exit 0
fi