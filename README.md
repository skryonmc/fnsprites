# Fortnite Sprites v2 (Fabric 1.21.1)

Compilar: JDK 21 + Gradle 8.8+  ->  `gradle build`  (jar en build/libs/). Probar: `gradle runClient`.

## Contenido
- 16 espíritus x 3 acabados (Base, Dorado, Cheat Master) = 48 objetos, pestaña "Espíritus de Fortnite".
- Cada espíritu tiene modelo 3D propio y un sonido propio (src/main/resources/assets/fnsprites/sounds/).
- Entidad "Espíritu": vaga por el Overworld, hace su sonido, se recoge con clic derecho.
- Santuario (fnsprites:shrine): se genera solo cerca de los jugadores. Ofrece un espíritu -> Bendición + aparecen espíritus.
- Equipar: clic derecho (pasa a la mano secundaria). Niveles 1-5.

## Comandos (OP)
/fnsprites spawn <espiritu> [gold|cheat_master]
/fnsprites shrine

## Cambiar sonidos
Reemplaza los .ogg (mono) en assets/fnsprites/sounds/ conservando el nombre.
Si ves al espíritu de espaldas: SpriteRenderer.java, cambia 180.0f por 0.0f.
