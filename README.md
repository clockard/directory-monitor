# Directory Monitor
## Overview
Directory monitor does just what it says monitors a filesystem directory for file creates, updates, and deletes.
Key features are:
- Remembers between restarts where it was and process any files that were changed since it last ran
- Allows association of a forgien key with each file 
- Allows file name pattern include/exclude
- Configurable retry period for files that fail to process
- Plugable file processors
