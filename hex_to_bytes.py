import argparse

def hex_to_decimal_bytes(hex_string):
    try:
        # Convert hex string to bytes
        byte_data = bytes.fromhex(hex_string)
        # Convert each byte to its decimal form
        decimal_values = [b for b in byte_data]
        return decimal_values
    except ValueError:
        raise ValueError("Invalid hex string")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert a hex string to decimal byte values.")
    parser.add_argument("hex_string", help="The hex string to convert")
    args = parser.parse_args()

    try:
        decimal_bytes = hex_to_decimal_bytes(args.hex_string)
        print("Decimal values:", decimal_bytes)
    except ValueError as e:
        print(e)