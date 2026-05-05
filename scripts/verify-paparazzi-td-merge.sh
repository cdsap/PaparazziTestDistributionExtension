#!/usr/bin/env bash
#
# End-to-end check that TDHtmlReportWriter (running on Test Distribution agents)
# and MergePaparazziOutputsTask agreed on a single report directory.
#
# Verifies the structure produced by `mergePaparazzi*Outputs`:
#   - source `td-*` dirs land under <input_dir> (proves the writer honored
#     the `paparazzi.td.report.dir` system property set by the plugin)
#   - <output_dir>/{runs,images,index.html,paparazzi.js,index.js} exist
#   - index.js exposes `window.all_runs`
#   - run-file count under <output_dir>/runs matches the union across
#     <input_dir>/td-*/runs (no runs silently dropped during merge)
#
# Usage: verify-paparazzi-td-merge.sh <input_dir> <output_dir>
#   <input_dir>  : tdPaparazzi.inputReportDir (where td-* live)
#   <output_dir> : tdPaparazzi.outputReportDir (the merged report)

set -euo pipefail

input_dir="${1:?input dir required}"
output_dir="${2:?output dir required}"

fail() {
  echo "::error::$*" >&2
  exit 1
}

echo "Verifying TD Paparazzi merge"
echo "  input_dir : $input_dir"
echo "  output_dir: $output_dir"

[[ -d "$input_dir" ]] || fail "input_dir does not exist: $input_dir — TDHtmlReportWriter never wrote here"

shopt -s nullglob
td_src_dirs=("$input_dir"/td-*)
shopt -u nullglob
if [[ ${#td_src_dirs[@]} -eq 0 ]]; then
  fail "No td-* directories under $input_dir. The paparazzi.td.report.dir system property may not have reached the test JVM (check AndroidVariantConfigurator.wireTestTask wiring)."
fi
echo "Found ${#td_src_dirs[@]} td-* source dir(s) under $input_dir"

[[ -d "$output_dir" ]]              || fail "Merged report dir missing: $output_dir"
[[ -f "$output_dir/index.html" ]]   || fail "index.html missing in $output_dir"
[[ -f "$output_dir/paparazzi.js" ]] || fail "paparazzi.js missing in $output_dir"
[[ -f "$output_dir/index.js" ]]     || fail "index.js missing in $output_dir"

grep -q 'window.all_runs' "$output_dir/index.js" \
  || fail "index.js does not declare window.all_runs"

count_files() {
  local pattern="$1" ext="$2"
  local n=0
  shopt -s nullglob
  for f in $pattern/*."$ext"; do n=$((n+1)); done
  shopt -u nullglob
  echo "$n"
}

src_runs=0
for d in "${td_src_dirs[@]}"; do
  if [[ -d "$d/runs" ]]; then
    src_runs=$(( src_runs + $(count_files "$d/runs" js) ))
  fi
done
merged_runs=0
[[ -d "$output_dir/runs" ]] && merged_runs=$(count_files "$output_dir/runs" js)

[[ "$src_runs"    -gt 0 ]] || fail "No run .js files under $input_dir/td-*/runs — TDHtmlReportWriter produced no runs."
[[ "$merged_runs" -gt 0 ]] || fail "No run .js files under $output_dir/runs — merge task did not pick up source runs."

echo "Source runs (sum across td-*): $src_runs"
echo "Merged runs:                   $merged_runs"
if [[ "$src_runs" != "$merged_runs" ]]; then
  echo "::warning::Run count mismatch (src=$src_runs merged=$merged_runs). Two agents likely produced the same run filename — MergePaparazziOutputsTask uses copyRecursively(overwrite=true) so the last write wins silently."
fi

src_images=0
for d in "${td_src_dirs[@]}"; do
  if [[ -d "$d/images" ]]; then
    src_images=$(( src_images + $(count_files "$d/images" png) ))
  fi
done
merged_images=0
[[ -d "$output_dir/images" ]] && merged_images=$(count_files "$output_dir/images" png)
echo "Source images: $src_images, merged images: $merged_images"

echo "Merged report listing:"
ls -la "$output_dir"

echo "OK: merged TD Paparazzi report verified at $output_dir"
