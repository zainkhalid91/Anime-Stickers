"""
Read `adb shell dumpsys batterystats` output and report one app's battery use.

    python tools/battery_report.py spike/out/batterystats_20min.txt --uid u0a558 --capacity 4448

Prints the measured period, the app's estimated mAh, and that as % of battery per hour.
"""
import argparse
import re


def parse_duration(text: str) -> float:
    """'20m 3s 120ms' -> seconds."""
    total = 0.0
    for value, unit in re.findall(r"(\d+)(h|ms|m|s)", text):
        v = int(value)
        total += {"h": 3600, "m": 60, "s": 1, "ms": 0.001}[unit] * v
    return total


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file")
    ap.add_argument("--uid", required=True, help="e.g. u0a558")
    ap.add_argument("--capacity", type=float, required=True, help="battery capacity in mAh")
    args = ap.parse_args()
    s = open(args.file, encoding="utf-8", errors="ignore").read()

    # Period the stats cover (time on battery since reset).
    m = re.search(r"Time on battery: ([0-9hms ]+)\(", s)
    secs = parse_duration(m.group(1)) if m else None

    # Per-UID power, e.g. "UID u0a558: 1.23 fg: ... ( cpu=0.81 ... )"
    line = next((l.strip() for l in s.splitlines() if l.strip().startswith(f"UID {args.uid}:")), None)
    print("period:", m.group(1).strip() if m else "unknown")
    print("line:", line or "not found")
    if line and secs:
        mah = float(re.search(r"UID \S+: ([0-9.]+)", line).group(1))
        per_hour = mah * 3600 / secs
        print(f"app: {mah:.3f} mAh in {secs / 60:.1f} min -> {per_hour:.2f} mAh/h "
              f"= {per_hour / args.capacity * 100:.3f}% of battery per hour")

    # CPU time of the app's process, as a sanity check.
    # Only look inside this app's own block (it ends at the next "  uXXX:" header).
    block = re.search(rf"^  {args.uid}:\n(.*?)(?=^  \S+:\n)", s, re.S | re.M)
    cpu = re.search(r"Total cpu time: u=([0-9a-z ]+) s=([0-9a-z ]+)", block.group(1)) if block else None
    if cpu:
        print("cpu user:", cpu.group(1).strip(), "| system:", cpu.group(2).strip())


if __name__ == "__main__":
    main()
