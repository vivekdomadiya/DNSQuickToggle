#!/usr/bin/env python3
"""
Automated version bumping script for Android app/build.gradle.
Increments versionCode and bumps versionName (patch, minor, or major).
"""

import argparse
import os
import re
import sys

def parse_args():
    parser = argparse.ArgumentParser(description="Bump Android versionCode and versionName")
    parser.add_argument(
        "--file",
        default="app/build.gradle",
        help="Path to app/build.gradle (default: app/build.gradle)",
    )
    parser.add_argument(
        "--type",
        choices=["patch", "minor", "major"],
        default="patch",
        help="SemVer bump type for versionName (default: patch)",
    )
    return parser.parse_args()

def bump_semver(version_str, bump_type):
    parts = version_str.split(".")
    while len(parts) < 3:
        parts.append("0")
    
    try:
        major, minor, patch = int(parts[0]), int(parts[1]), int(parts[2])
    except ValueError:
        raise ValueError(f"Cannot parse semver from versionName: '{version_str}'")

    if bump_type == "major":
        major += 1
        minor = 0
        patch = 0
    elif bump_type == "minor":
        minor += 1
        patch = 0
    else:  # patch
        patch += 1

    return f"{major}.{minor}.{patch}"

def main():
    args = parse_args()
    gradle_file = args.file

    if not os.path.isfile(gradle_file):
        print(f"Error: Gradle file not found at {gradle_file}", file=sys.stderr)
        sys.exit(1)

    with open(gradle_file, "r", encoding="utf-8") as f:
        content = f.read()

    # Find versionCode
    vc_match = re.search(r"versionCode\s+(\d+)", content)
    if not vc_match:
        print("Error: Could not find versionCode in file", file=sys.stderr)
        sys.exit(1)

    old_code = int(vc_match.group(1))
    new_code = old_code + 1
    content = re.sub(r"versionCode\s+\d+", f"versionCode {new_code}", content, count=1)

    # Find versionName
    vn_match = re.search(r"versionName\s+['\"]([^'\"]+)['\"]", content)
    if not vn_match:
        print("Error: Could not find versionName in file", file=sys.stderr)
        sys.exit(1)

    old_name = vn_match.group(1)
    new_name = bump_semver(old_name, args.type)
    content = re.sub(
        r"versionName\s+['\"][^'\"]+['\"]",
        f"versionName '{new_name}'",
        content,
        count=1,
    )

    with open(gradle_file, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"Successfully bumped {gradle_file}:")
    print(f"  versionCode: {old_code} -> {new_code}")
    print(f"  versionName: {old_name} -> {new_name}")

    # Set GitHub Actions output if running inside GitHub Actions
    github_output = os.getenv("GITHUB_OUTPUT")
    if github_output and os.path.exists(github_output):
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"new_version={new_name}\n")
            f.write(f"new_version_code={new_code}\n")

    # Set GitHub Actions environment variable if available
    github_env = os.getenv("GITHUB_ENV")
    if github_env and os.path.exists(github_env):
        with open(github_env, "a", encoding="utf-8") as f:
            f.write(f"NEW_VERSION={new_name}\n")
            f.write(f"NEW_VERSION_CODE={new_code}\n")

if __name__ == "__main__":
    main()
