# Pisan Commands — Fabric

`pisan_commands-1.1.5.jar` Paper eklentisinin Fabric sunucu modu olarak yeniden yazılmış sürümü.

## Hedef JAR sayısı

Minecraft/Fabric 26.1 ile Java ve obfuscation/remap modeli değiştiği için iki binary ailesi hedeflenir:

| JAR | Minecraft | Java |
|---|---|---|
| `pisan-commands-2.0.0-mc1.21.1-1.21.11.jar` | 1.21.1–1.21.11 | 21 |
| `pisan-commands-2.0.0-mc26.1-26.2.jar` | 26.1–26.2 | 25 |

Her ikisi de server-side Fabric modudur. Uygun Fabric Loader ve Fabric API gerekir.

## Port edilen komutlar

- `/pisanenchant <add|edit|auto|remove> <enchantment> [level]`
- `/pisanrename edit <isim>` / `/pisanrename reset`
- `/pisangive <vanilla give argümanları>` — alias: `/pg`, `/pgive`
- `/pisansummon <vanilla summon argümanları>` — alias: `/ps`, `/psummon`
- `/pisanmaxminecartspeed [blok/s]` — alias: `/pmcs`, `/pminecartspeed`

Paper'a özel DataComponent yardımcıları yerine Fabric sürümünde mümkün olduğunca vanilla komut/component altyapısı kullanılır. Bu, ara Minecraft sürümlerinde binary uyumluluğu artırır.

## Yeni sunucu yönetimi

`/pisan` ana komutu vanilla OP kontrolünü kullanır; ekstra permission modu gerektirmez.

- `/pisan save`, `/pisan list`
- `/pisan day`, `/pisan night`
- `/pisan clear`, `/pisan rain`, `/pisan thunder`
- `/pisan gm ...`, `/pisan tp ...`
- `/pisan kick ...`, `/pisan ban ...`, `/pisan pardon ...`
- `/pisan op ...`, `/pisan deop ...`, `/pisan whitelist ...`
- `/pisan difficulty ...`, `/pisan gamerule ...`
- `/pisan heal [hedef]`, `/pisan feed [hedef]`
- `/pisan run <herhangi bir vanilla komut>`

Kısayollar aynı `CommandSourceStack` ile vanilla command dispatcher'a yönlendirilir. Böylece vanilla komutların kendi doğrulama/yetki kuralları korunur.

## Config

İlk açılışta `config/pisan_commands.properties` oluşturulur:

```properties
minecart-max-speed-blocks-per-second=8.0
```

`/pisanmaxminecartspeed 20` ile restart gerektirmeden güncellenebilir.

## Build

```bash
gradle :legacy:build
gradle :modern:build
```

## CI / uyumluluk testi

`.github/workflows/ci.yml` iki JAR'ı derler ve dedicated Fabric server smoke testini şu sürümlerde çalıştıracak şekilde hazırlanmıştır:

`1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2`

Smoke test mod entrypoint'ini, linkage/mixin hatalarını, sunucunun `Done` aşamasına gelmesini ve temel yönetim komutlarının konsoldan çalıştırılmasını kontrol eder.
