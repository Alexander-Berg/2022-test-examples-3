#!/usr/bin/awk -f

{ print_line = 0 }

# Include statements
/ \/images/ { print_line = 1 }

# Exclude statements


# Final actions
print_line == 1 { print }
