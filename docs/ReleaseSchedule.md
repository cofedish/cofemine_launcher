# Release Schedule

<!-- #BEGIN LANGUAGE_SWITCHER -->
**English** | [Chinese](ReleaseSchedule_zh.md)
<!-- #END LANGUAGE_SWITCHER -->

CofeMine Launcher releases are published on GitHub and follow semantic versioning.

## Versioning

- Versions use the `vX.Y.Z` format.
- Pushing a tag triggers the release workflow and generates signed update metadata.
- There is no fixed calendar schedule; releases are made when changes are ready.

## Update Channel

The launcher checks GitHub Releases for updates. The update metadata is published as `update.json` in each release.

## Pre-releases

If we need early testing, we use pre-release tags like `vX.Y.Z-beta.1` and mark the release as a pre-release on GitHub.
