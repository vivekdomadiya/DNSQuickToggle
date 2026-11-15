#!/usr/bin/env bash
set -euo pipefail

# 1. Run Python version bumper
BUMP_TYPE="${1:-patch}"
echo "==> Running version bump (type: ${BUMP_TYPE})..."
python3 .github/scripts/bump-version.py --type "${BUMP_TYPE}"

# 2. Extract new version info from app/build.gradle
NEW_VERSION=$(grep "versionName" app/build.gradle | head -n 1 | awk -F "['\"]" '{print $2}')
NEW_VERSION_CODE=$(grep "versionCode" app/build.gradle | head -n 1 | awk '{print $2}')
echo "==> Bumped to versionName: ${NEW_VERSION}, versionCode: ${NEW_VERSION_CODE}"

# 3. Configure git committer identity
git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"

# 4. Create bump branch, commit, and push
BUMP_BRANCH="chore/bump-version-${NEW_VERSION}"
echo "==> Creating branch ${BUMP_BRANCH}..."
git checkout -b "$BUMP_BRANCH"
git add app/build.gradle
git commit -m "chore(release): bump version to ${NEW_VERSION} [skip ci]"
git push origin "$BUMP_BRANCH"

# 5. Open and squash-merge the automated PR
echo "==> Creating PR for bump..."
gh pr create \
  --base master \
  --head "$BUMP_BRANCH" \
  --title "chore(release): bump version to ${NEW_VERSION} [skip ci]" \
  --body "Automated version bump to ${NEW_VERSION} (code: ${NEW_VERSION_CODE}) after release."

echo "==> Merging bump PR..."
gh pr merge "$BUMP_BRANCH" --squash --delete-branch
echo "==> Version bump PR merged successfully!"
