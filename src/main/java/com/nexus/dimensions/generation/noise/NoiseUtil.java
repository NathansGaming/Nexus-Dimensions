/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation.noise;

import java.util.Random;

public final class NoiseUtil {
    private final int[] perm = new int[512];

    public NoiseUtil(long seed) {
        int i;
        int[] p = new int[256];
        for (int i2 = 0; i2 < 256; ++i2) {
            p[i2] = i2;
        }
        Random random = new Random(seed);
        for (i = 255; i > 0; --i) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (i = 0; i < 512; ++i) {
            this.perm[i] = p[i & 0xFF];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        double u;
        int h = hash & 0xF;
        double d = u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double perlin3(double x, double y, double z) {
        int X = (int)Math.floor(x) & 0xFF;
        int Y = (int)Math.floor(y) & 0xFF;
        int Z = (int)Math.floor(z) & 0xFF;
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        double u = NoiseUtil.fade(x);
        double v = NoiseUtil.fade(y);
        double w = NoiseUtil.fade(z);
        int a = this.perm[X] + Y;
        int aa = this.perm[a] + Z;
        int ab = this.perm[a + 1] + Z;
        int b = this.perm[X + 1] + Y;
        int ba = this.perm[b] + Z;
        int bb = this.perm[b + 1] + Z;
        return NoiseUtil.lerp(w, NoiseUtil.lerp(v, NoiseUtil.lerp(u, NoiseUtil.grad(this.perm[aa], x, y, z), NoiseUtil.grad(this.perm[ba], x - 1.0, y, z)), NoiseUtil.lerp(u, NoiseUtil.grad(this.perm[ab], x, y - 1.0, z), NoiseUtil.grad(this.perm[bb], x - 1.0, y - 1.0, z))), NoiseUtil.lerp(v, NoiseUtil.lerp(u, NoiseUtil.grad(this.perm[aa + 1], x, y, z - 1.0), NoiseUtil.grad(this.perm[ba + 1], x - 1.0, y, z - 1.0)), NoiseUtil.lerp(u, NoiseUtil.grad(this.perm[ab + 1], x, y - 1.0, z - 1.0), NoiseUtil.grad(this.perm[bb + 1], x - 1.0, y - 1.0, z - 1.0))));
    }

    public double perlin2(double x, double y) {
        return this.perlin3(x, y, 0.0);
    }

    public double fbm2D(double x, double y, double frequency, int octaves, double lacunarity, double gain, boolean ridged, double warp) {
        if (warp > 1.0E-4) {
            double wx = this.perlin2((x + 1000.0) * frequency * 0.5, (y - 1000.0) * frequency * 0.5);
            double wy = this.perlin2((x - 500.0) * frequency * 0.5, (y + 500.0) * frequency * 0.5);
            x += wx * warp / frequency;
            y += wy * warp / frequency;
        }
        double amplitude = 1.0;
        double freq = frequency;
        double sum = 0.0;
        double norm = 0.0;
        for (int i = 0; i < octaves; ++i) {
            double n = this.perlin2(x * freq, y * freq);
            if (ridged) {
                n = 1.0 - Math.abs(n);
                n *= n;
            }
            sum += n * amplitude;
            norm += amplitude;
            amplitude *= gain;
            freq *= lacunarity;
        }
        return norm > 0.0 ? sum / norm : 0.0;
    }

    public double fbm3D(double x, double y, double z, double frequency, int octaves, double lacunarity, double gain) {
        double amplitude = 1.0;
        double freq = frequency;
        double sum = 0.0;
        double norm = 0.0;
        for (int i = 0; i < octaves; ++i) {
            sum += this.perlin3(x * freq, y * freq, z * freq) * amplitude;
            norm += amplitude;
            amplitude *= gain;
            freq *= lacunarity;
        }
        return norm > 0.0 ? sum / norm : 0.0;
    }

    public double worley2D(double x, double y, double frequency, double jitter) {
        double px = x * frequency;
        double py = y * frequency;
        int cellX = (int)Math.floor(px);
        int cellY = (int)Math.floor(py);
        double minDist = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                double jitterY;
                double pointY;
                double ddy;
                int cx = cellX + dx;
                int cy = cellY + dy;
                double jitterX = this.hash01(cx, cy, 1) * jitter;
                double pointX = (double)cx + 0.5 + (jitterX - 0.5);
                double ddx = px - pointX;
                double dist = Math.sqrt(ddx * ddx + (ddy = py - (pointY = (double)cy + 0.5 + ((jitterY = this.hash01(cx, cy, 2) * jitter) - 0.5))) * ddy);
                if (!(dist < minDist)) continue;
                minDist = dist;
            }
        }
        return Math.min(1.0, minDist);
    }

    public double worley3D(double x, double y, double z, double frequency, double jitter) {
        double px = x * frequency;
        double py = y * frequency;
        double pz = z * frequency;
        int cellX = (int)Math.floor(px);
        int cellY = (int)Math.floor(py);
        int cellZ = (int)Math.floor(pz);
        double minDist = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    double jitterZ;
                    double pointZ;
                    double ddz;
                    double jitterY;
                    double pointY;
                    double ddy;
                    int cx = cellX + dx;
                    int cy = cellY + dy;
                    int cz = cellZ + dz;
                    double jitterX = this.hash013(cx, cy, cz, 1) * jitter;
                    double pointX = (double)cx + 0.5 + (jitterX - 0.5);
                    double ddx = px - pointX;
                    double dist = Math.sqrt(ddx * ddx + (ddy = py - (pointY = (double)cy + 0.5 + ((jitterY = this.hash013(cx, cy, cz, 2) * jitter) - 0.5))) * ddy + (ddz = pz - (pointZ = (double)cz + 0.5 + ((jitterZ = this.hash013(cx, cy, cz, 3) * jitter) - 0.5))) * ddz);
                    if (!(dist < minDist)) continue;
                    minDist = dist;
                }
            }
        }
        return Math.min(1.5, minDist);
    }

    private double hash01(int x, int y, int salt) {
        long h = (long)x * 374761393L + (long)y * 668265263L + (long)salt * Integer.MAX_VALUE;
        h = (h ^ h >>> 13) * 1274126177L;
        h ^= h >>> 16;
        return (double)(h & 0xFFFFFFL) / 1.6777215E7;
    }

    private double hash013(int x, int y, int z, int salt) {
        long h = (long)x * 374761393L + (long)y * 668265263L + (long)z * 2246822519L + (long)salt * 3266489917L;
        h = (h ^ h >>> 13) * 1274126177L;
        h ^= h >>> 16;
        return (double)(h & 0xFFFFFFL) / 1.6777215E7;
    }
}
