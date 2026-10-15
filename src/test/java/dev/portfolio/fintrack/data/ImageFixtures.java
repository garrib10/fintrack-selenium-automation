package dev.portfolio.fintrack.data;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Images created at runtime, so the repository holds no binary fixtures,
 * personal photos, or embedded metadata.
 */
public final class ImageFixtures {

    private static final int PROFILE_PHOTO_SIZE = 64;

    private static final int FILL_RGB = 0x1D76DB;

    private ImageFixtures() {
        // Utility class; prevent object creation.
    }

    /**
     * Writes a small solid-color PNG (64×64, well under 1 KB) into
     * {@code directory}. ImageIO's PNG writer adds no text or EXIF chunks.
     */
    public static Path writeProfilePhoto(Path directory) throws IOException {
        BufferedImage image = new BufferedImage(
                PROFILE_PHOTO_SIZE,
                PROFILE_PHOTO_SIZE,
                BufferedImage.TYPE_INT_RGB);

        // Plain pixel writes: no AWT toolkit, so no display is needed.
        for (int y = 0; y < PROFILE_PHOTO_SIZE; y++) {
            for (int x = 0; x < PROFILE_PHOTO_SIZE; x++) {
                image.setRGB(x, y, FILL_RGB);
            }
        }

        Path file = directory.resolve("selenium-profile-photo.png");

        if (!ImageIO.write(image, "png", file.toFile())) {
            throw new IOException("No PNG writer is available");
        }

        return file;
    }
}
