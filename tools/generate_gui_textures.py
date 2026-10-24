#!/usr/bin/env python3
"""Generates the Head Index menu backgrounds.

The backgrounds are bitmap font glyphs that are drawn through the container title (see GuiTitle.java), so the
texture has to line up with the vanilla container layout:

  * the image is the whole container, including the player inventory (176 x (114 + rows * 18))
  * container slots sit at (8 + 18 * col, 18 + 18 * row)
  * player slots sit at (8 + 18 * col, 103 + (rows - 4) * 18 + 18 * row), the hotbar 58px below the first row
  * every slot is cut out (fully transparent) because the title is rendered *above* the items. The vanilla
    slot background shows through the cutout.

Usage: python3 -I tools/generate_gui_textures.py
"""
from pathlib import Path

from PIL import Image

WIDTH = 176

BORDER = (28, 33, 48, 255)
BASE = (201, 207, 220, 255)
HIGHLIGHT = (240, 243, 250, 255)
SHADOW = (130, 139, 158, 255)
HEADER = (44, 56, 89, 255)
ACCENT = (91, 141, 239, 255)
SLOT_DARK = (75, 84, 104, 255)
SLOT_LIGHT = (244, 246, 251, 255)
NAV_DARK = (44, 56, 89, 255)
NAV_LIGHT = (143, 177, 247, 255)

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/headindex/textures/gui"


def rect(img, x0, y0, x1, y1, color):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            img.putpixel((x, y), color)


def slot(img, x, y, dark=SLOT_DARK, light=SLOT_LIGHT):
    """Draws the 18x18 frame around the 16x16 slot whose item origin is (x, y)."""
    left, top, right, bottom = x - 1, y - 1, x + 16, y + 16
    rect(img, left, top, right, top, dark)
    rect(img, left, top, left, bottom, dark)
    rect(img, left, bottom, right, bottom, light)
    rect(img, right, top, right, bottom, light)
    rect(img, left, bottom, left, bottom, dark)
    rect(img, right, top, right, top, dark)
    rect(img, x, y, x + 15, y + 15, (0, 0, 0, 0))


def panel(rows, nav_row=None):
    height = 114 + rows * 18
    img = Image.new("RGBA", (WIDTH, height), (0, 0, 0, 0))

    rect(img, 0, 0, WIDTH - 1, height - 1, BORDER)
    rect(img, 1, 1, WIDTH - 2, height - 2, BASE)
    rect(img, 1, 1, WIDTH - 2, 1, HIGHLIGHT)
    rect(img, 1, 1, 1, height - 2, HIGHLIGHT)
    rect(img, 2, height - 2, WIDTH - 2, height - 2, SHADOW)
    rect(img, WIDTH - 2, 2, WIDTH - 2, height - 2, SHADOW)

    # header bar behind the title text (which starts at x = 8, y = 6)
    rect(img, 3, 3, WIDTH - 4, 14, HEADER)
    rect(img, 3, 15, WIDTH - 4, 15, ACCENT)

    # rounded outer corners
    for x, y in ((0, 0), (WIDTH - 1, 0), (0, height - 1), (WIDTH - 1, height - 1)):
        img.putpixel((x, y), (0, 0, 0, 0))

    for row in range(rows):
        for col in range(9):
            if row == nav_row:
                slot(img, 8 + 18 * col, 18 + 18 * row, NAV_DARK, NAV_LIGHT)
            else:
                slot(img, 8 + 18 * col, 18 + 18 * row)

    inventory_y = 103 + (rows - 4) * 18
    for row in range(3):
        for col in range(9):
            slot(img, 8 + 18 * col, inventory_y + 18 * row)
    for col in range(9):
        slot(img, 8 + 18 * col, inventory_y + 58)

    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    panel(2).save(OUT / "menu_9x2.png", optimize=True)
    panel(6, nav_row=5).save(OUT / "menu_9x6.png", optimize=True)


if __name__ == "__main__":
    main()
