# Changelog

## Unreleased
- Added CLI flags `--remap-backup`/`--no-remap-backup` and `--remap-compute-frames` to control line-number remap backups and frame computation (for legacy versions where remap is enabled).
- Wired remap backup/frame settings through the decompiler and result saver so CLI choices take effect.
- Remap remains disabled by default for PZ 42.13+ due to jar rewrite limitations (see README).
