#!/usr/bin/env bash
# 同步本地 docs-site 改动到线上服务器 /var/www/html/aiverse/
# 用法: bash ~/workspace/scripts/deploy_docs.sh <本地相对路径> [<更多相对路径>...]
# 说明: 文件属主为 www-data，ubuntu 用户对部分文件无写权限，用 sudo -n tee 覆写。
set -euo pipefail

REMOTE="主机"
BASE="/var/www/html/aiverse"
LOCAL_BASE="$HOME/workspace/docs-site"

if [ $# -lt 1 ]; then
  echo "用法: $0 <相对 docs-site 的路径> ..." >&2
  exit 1
fi

for rel in "$@"; do
  local_path="$LOCAL_BASE/$rel"
  remote_path="$BASE/$rel"
  if [ ! -f "$local_path" ]; then
    echo "跳过(本地不存在): $rel" >&2
    continue
  fi
  # 先备份线上原文件，再覆写
  ssh "$REMOTE" "sudo -n cp -p '$remote_path' '${remote_path}.bak.\$(date +%Y%m%d-%H%M%S)' 2>/dev/null || true"
  ssh "$REMOTE" "sudo -n tee '$remote_path' > /dev/null" < "$local_path"
  # 校验远端 md5 与本地一致
  local_md5=$(md5sum "$local_path" | awk '{print $1}')
  remote_md5=$(ssh "$REMOTE" "sudo -n md5sum '$remote_path'" | awk '{print $1}')
  if [ "$local_md5" = "$remote_md5" ]; then
    echo "OK  $rel  ($local_md5)"
  else
    echo "MISMATCH $rel local=$local_md5 remote=$remote_md5" >&2
    exit 1
  fi
done
echo "部署完成。"