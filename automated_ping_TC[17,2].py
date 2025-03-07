from yamcs.client import YamcsClient
import time

client = YamcsClient('localhost:8090')

mdb = client.get_mdb(instance='PeakSat')
# ...

archive = client.get_archive(instance='PeakSat')
# ...

processor = client.get_processor(instance='PeakSat', processor='realtime')
# ...

count = 0

while True:
    count += 1
    ping_command = processor.issue_command('ST[17]/TC(17,1)_are_you_alive_connection', args={"application_process_ID": 'OBC'})
    print(f"Issue #{count}: ", ping_command)
    time.sleep(10)
    count += 1
    ping_command = processor.issue_command('ST[17]/TC(17,1)_are_you_alive_connection', args={"application_process_ID": 'COMMS'})
    print(f"Issue #{count}: ", ping_command)
    time.sleep(10)