#!/usr/bin/env bash
set -euo pipefail
MC_VERSION="${1:?Minecraft version required}"
MOD_JAR="${2:?Pisan Commands jar required}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/mods"
cp "$MOD_JAR" "$WORK/mods/pisan-commands.jar"

FABRIC_META='https://meta.fabricmc.net/v2'
LOADER="$(curl -fsSL "$FABRIC_META/versions/loader/$MC_VERSION" | jq -r '[.[] | select(.loader.stable == true)][0].loader.version // .[0].loader.version')"
INSTALLER="$(curl -fsSL "$FABRIC_META/versions/installer" | jq -r '[.[] | select(.stable == true)][0].version // .[0].version')"
curl -fsSL "$FABRIC_META/versions/loader/$MC_VERSION/$LOADER/$INSTALLER/server/jar" -o "$WORK/server.jar"

API_JSON="$(curl -fsSG 'https://api.modrinth.com/v2/project/P7dR8mSH/version' \
  --data-urlencode "game_versions=[\"$MC_VERSION\"]" \
  --data-urlencode 'loaders=["fabric"]')"
API_URL="$(jq -r '[.[] | select(.version_type == "release")][0].files[] | select(.primary == true).url' <<<"$API_JSON" | head -1)"
if [[ -z "$API_URL" || "$API_URL" == "null" ]]; then
  API_URL="$(jq -r '.[0].files[0].url' <<<"$API_JSON")"
fi
curl -fsSL "$API_URL" -o "$WORK/mods/fabric-api.jar"

cat > "$WORK/eula.txt" <<EOT
eula=true
EOT
cat > "$WORK/server.properties" <<EOT
online-mode=false
motd=Pisan Commands CI
max-players=1
view-distance=3
simulation-distance=3
spawn-protection=0
EOT

pushd "$WORK" >/dev/null
set +e
(
  sleep 75
  printf 'pisan help\npisanmaxminecartspeed 12\npisan day\nstop\n'
) | timeout 150s java -Xms512M -Xmx1536M -jar server.jar nogui >server.log 2>&1
CODE=$?
set -e
cat server.log

if ! grep -q 'Pisan Commands Fabric loaded' server.log; then
  echo "Pisan Commands did not initialize on $MC_VERSION" >&2
  exit 1
fi
if grep -Eq 'Mixin apply failed|Mod resolution encountered an incompatible mod set|Could not execute entrypoint|NoClassDefFoundError|NoSuchMethodError|NoSuchFieldError|InvalidMixinException' server.log; then
  echo "Compatibility failure detected on $MC_VERSION" >&2
  exit 1
fi
if ! grep -q 'Done (' server.log; then
  echo "Server never reached Done on $MC_VERSION (exit $CODE)" >&2
  exit 1
fi
popd >/dev/null
