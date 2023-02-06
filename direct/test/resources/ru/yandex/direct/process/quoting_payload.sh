#!/bin/sh
for x in 1 2; do
   # comment inside code
   echo x\\ '"'\'$x\''"' $(hostname) $(whoami) >> /volume/test_file
done
