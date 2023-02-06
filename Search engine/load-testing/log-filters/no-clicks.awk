#!/usr/bin/awk -f 

{ print_line = 1 }

# Include statements

# Exclude statements
/ \/clck/ { print_line = 0 }

# Final actions
print_line == 1 { print }
