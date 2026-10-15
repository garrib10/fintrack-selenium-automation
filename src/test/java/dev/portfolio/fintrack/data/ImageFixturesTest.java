package dev.portfolio.fintrack.data;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("unit")
class ImageFixturesTest {

    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @TempDir
    Path directory;

    @Test
    void profilePhotoIsATinyMetadataFreePng() throws Exception {
        Path file = ImageFixtures.writeProfilePhoto(directory);
        byte[] bytes = Files.readAllBytes(file);
        String chunks = new String(bytes, StandardCharsets.ISO_8859_1);
        BufferedImage image = ImageIO.read(file.toFile());

        assertAll(
                () -> assertTrue(file.startsWith(directory), "Expected the file in the temp directory"),
                () -> assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(bytes, 8)),
                () -> assertTrue(bytes.length < 1024, "Expected under 1 KB, was " + bytes.length),
                () -> assertEquals(64, image.getWidth()),
                () -> assertEquals(64, image.getHeight()),
                () -> assertFalse(chunks.contains("tEXt") || chunks.contains("iTXt")
                        || chunks.contains("zTXt") || chunks.contains("eXIf"),
                        "Expected no text or EXIF metadata chunks"));
    }
}
