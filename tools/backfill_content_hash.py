#!/usr/bin/env python3
"""Fill in contentHash for mios stored before the column existed, and list duplicates.

Usage: backfill_content_hash.py <dataDirectory>

Run it once after deploying, with the server stopped or idle. It only updates
rows where contentHash is NULL and never deletes anything: duplicates are printed
so you can review them (keep one hash per group, drop the rest via the admin tools).
The zeroed ranges must match MioStorage.computeContentHash.
"""
import hashlib
import io
import sqlite3
import sys
import zipfile
from pathlib import Path

ZEROED = [(0x10, 0x12), (0xAC, 0xAE), (0xCF, 0x100)]
TABLES = {"Games": "game", "Manga": "manga", "Records": "record"}


def content_hash(data: bytes) -> str:
    buf = bytearray(data)
    for start, end in ZEROED:
        buf[start:end] = bytes(end - start)
    return hashlib.md5(buf).hexdigest()


def main(data_dir: str) -> None:
    data = Path(data_dir)
    db = sqlite3.connect(data / "mioDatabase.sqlite")
    for table, folder in TABLES.items():
        try:
            db.execute(f"ALTER TABLE {table} ADD COLUMN contentHash TEXT")
        except sqlite3.OperationalError:
            pass  # column already there

        rows = db.execute(f"SELECT hash FROM {table} WHERE contentHash IS NULL").fetchall()
        done = missing = 0
        for (h,) in rows:
            path = data / "mio" / folder / f"{h}.miozip"
            try:
                with zipfile.ZipFile(path) as z:
                    mio = z.read(z.namelist()[0])
            except (OSError, zipfile.BadZipFile, IndexError):
                missing += 1
                print(f"{table}: cannot read {path}", file=sys.stderr)
                continue
            db.execute(f"UPDATE {table} SET contentHash = ? WHERE hash = ?", (content_hash(mio), h))
            done += 1
        db.commit()
        print(f"{table}: {done} updated, {missing} unreadable")

        groups = db.execute(
            f"SELECT contentHash, group_concat(hash || ' ' || name, ' | ') FROM {table} "
            "WHERE contentHash IS NOT NULL GROUP BY contentHash HAVING count(*) > 1"
        ).fetchall()
        for ch, members in groups:
            print(f"  duplicate {ch}: {members}")
    db.close()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
