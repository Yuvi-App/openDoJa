package com.nttdocomo.ui;

/**
 * Host-side helpers for Image contract checks that are not part of the DoJa API surface.
 */
public final class _ImageInternalAccess {
    private _ImageInternalAccess() {
    }

    public static boolean isDisposed(Image image) {
        if (image == null) {
            return false;
        }
        try {
            image.getWidth();
            return false;
        } catch (UIException e) {
            if (e.getStatus() == UIException.ILLEGAL_STATE) {
                return true;
            }
            throw e;
        }
    }

    public static void requireUsable(Image image) {
        if (image == null) {
            return;
        }
        image.getWidth();
        image.getHeight();
    }
}
