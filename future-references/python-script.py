from yamcs.client import YamcsClient
from datetime import datetime, timedelta, timezone, tzinfo
from itertools import islice

def print_last_values():
    """Print the last 10 values."""
    iterable = archive.list_parameter_values(
        "/AcubeSAT/Status_Temperature_1", descending=True
    )
    for pval in islice(iterable, 0, 10):
        print(pval)

def iterate_specific_parameter_range():
    """Count the number of parameter values in a specific range."""
    now = datetime.now(tz=timezone.utc)
    start = now - timedelta(hours=1)
    print(now)
    print(start)
    total = 0
    for pval in archive.list_parameter_values(
        "/AcubeSAT/Status_Temperature_1", start=start, stop=now
    ):
        total += 1
        print(pval)
    print("Found", total, "parameter values in range")



client = YamcsClient('localhost:8090')
archive = client.get_archive(instance='myproject')


if __name__ == "__main__":
    iterate_specific_parameter_range()
#/usr/lib/python3/dist-packages/google/protobuf/internal$ python well_known_types.py