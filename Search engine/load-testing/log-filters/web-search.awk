#!/usr/bin/awk -f 

{ print_line = 0 }

# Include statements
/ \/yandsearch/ { print_line = 1 }
/ \/search/ { print_line = 1 }
/ \/familysearch/ { print_line = 1 }
/ \/schoolsearch/ { print_line = 1 }
/ \/msearch/ { print_line = 1 }
/ \/telsearch/ { print_line = 1 }
/ \/touchsearch/ { print_line = 1 }
/ \/padsearch/ { print_line = 1 }
/ \/largesearch/ { print_line = 1 }

# Exclude statements


# Final actions
print_line == 1 { print }
