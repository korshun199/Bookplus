#!/usr/bin/env bash
set -e

mkdir -p app/src/main/assets
cat > app/src/main/assets/index.html <<'HTML'
<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>ЧМ 2026</title></head><body><h1>ЧМ 2026</h1><p>Турнирная таблица MVP</p></body></html>
HTML

git add .
git commit -m "Add WC 2026 standings MVP" || true
