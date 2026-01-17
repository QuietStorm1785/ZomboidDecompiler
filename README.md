# Zomboid Decompiler
Simplified decompilation tool for Project Zomboid powered by [Vineflower](https://github.com/Vineflower/vineflower).
## Usage
### Windows
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .zip from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Extract the zip.
4) Navigate to `bin/` and run `ZomboidDecompiler.bat`.
5) Wait a few minutes for decompilation to complete. The black box will close when the program has finished.  
   - If you receive an error about not being able to find the game directory, open your command line to the `bin` folder and execute ``ZomboidDecompiler.bat "PATH"``, replacing `PATH` with the path to your game installation's `ProjectZomboid` folder.
     - Example: ``ZomboidDecompiler.bat "D:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"``

The decompiled source code will be written to `output/`, along with the dependencies and game jar.

### Other
1) Install [Java 17](https://www.oracle.com/fr/java/technologies/downloads/) or above.
2) Download the latest .zip from [Releases](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest).
3) Extract the zip.
4) Open your command line to the `bin` folder and execute ``ZomboidDecompiler "PATH"``, replacing `PATH` with the path to your game installation's `ProjectZomboid` folder.
   - Example: ``ZomboidDecompiler "D:\Program Files (x86)
   \Steam\steamapps\common\ProjectZomboid"``
Another example:
# Basic usage - includes all members with proper syntax
./ZomboidDecompiler "path/to/ProjectZomboid"

# With custom parameters to further control output
./ZomboidDecompiler "path/to/ProjectZomboid" -vf bytecode_source_mapping true

5) Wait a few minutes for decompilation to complete.
The decompiled source code will be written to `output/`, along with the dependencies and game jar.

## Features
- Single click game decompilation.
- Automatic gathering of game dependencies as decompilation context and for future recompilation.
- Renaming of function parameters using Rosetta data.
- Renaming of other variables according to type to enhance readability.
- Line number remapping for remote debugging.

## Line number remapping limitations
The CLI flag `--remap-line-numbers` is present for legacy versions but is currently **disabled** for Project Zomboid 42.13+.
- The tool cannot rewrite the game JAR in 42.13+, so bytecode→source mapping data cannot be injected.
- When the flag is supplied, a warning is emitted and no class files are modified.
- For debugging, use the decompiled sources directly and attach your IDE debugger to the running game.

## Version compatibility chart
Sometimes the game changes too much for Zomboid Decompiler to reasonably maintain compatibility with older versions.
Downloads for the latest version supporting certain game versions are listed here.

| Game version    | Last supporting version                                                               |
|-----------------|---------------------------------------------------------------------------------------|
| 42.13.0–latest  | [latest](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/latest)     |
| unknown–42.12.3 | [v0.2.3](https://github.com/demiurgeQuantified/ZomboidDecompiler/releases/tag/v0.2.3) |

## Command Line Interface
Launch with ``-h`` or ``--help`` for information about command line parameters.

## Remote debugging
A basic guide on using ZomboidDecompiler for remote debugging is hosted [here](https://github.com/demiurgeQuantified/PZModdingGuides/blob/main/guides/RemoteDebugging.md).

## Building
ZomboidDecompiler can be built with `gradlew build`.  
You can include Rosetta files in `src/main/resources/rosetta/` to be used as defaults when no rosetta directory is passed.
The standard binaries in Releases are built with the
[latest Rosetta data](https://github.com/PZ-Umbrella/pz-rosetta-source) included.
