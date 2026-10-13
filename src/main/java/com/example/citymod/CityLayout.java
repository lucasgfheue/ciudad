package com.example.citymod;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Define como es la ciudad. Todo se calcula a partir de las coordenadas del mundo,
 * asi que los chunks siempre encajan entre si.
 *
 * Cuadricula de manzanas de 48x48. En cada manzana:
 *   coordenadas locales 0..9  -> calle (acera 0-1, asfalto 2-7, acera 8-9)
 *   coordenadas locales 10..47 -> solar (edificio o parque)
 */
public final class CityLayout {
    public static final int CELL = 48;
    public static final int GROUND = 63;   // Y del bloque de superficie
    private static final int MIN_Y = -64;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState ASPHALT = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState LINE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState SIDEWALK = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState PAVING = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState INTERIOR_FLOOR = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState FLOOR_SLAB = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState POLE = Blocks.STONE_BRICK_WALL.defaultBlockState();
    private static final BlockState ROOF_FLAT = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState ROOF_HOUSE = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState LADDER =
            Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);

    private static final BlockState[] WALLS = {
            Blocks.BRICKS.defaultBlockState(),
            Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
            Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
            Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.QUARTZ_BLOCK.defaultBlockState(),
            Blocks.CYAN_TERRACOTTA.defaultBlockState()
    };
    private static final BlockState[] SKY_WALLS = {
            Blocks.GRAY_CONCRETE.defaultBlockState(),
            Blocks.BLACK_CONCRETE.defaultBlockState(),
            Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()
    };
    private static final BlockState[] HOUSE_WALLS = {
            Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.BRICKS.defaultBlockState(),
            Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
            Blocks.OAK_PLANKS.defaultBlockState()
    };
    private static final BlockState[] GLASSES = {
            Blocks.GLASS.defaultBlockState(),
            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
            Blocks.CYAN_STAINED_GLASS.defaultBlockState()
    };

    private final long seed;

    public CityLayout(long seed) {
        this.seed = seed;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private long cellHash(int cx, int cz) {
        return mix(seed ^ mix(cx * 341873128712L + cz * 132897987541L));
    }

    /** Datos de un solar. */
    private static final class Lot {
        boolean park;
        boolean house;
        int x0, x1, z0, z1;   // huella del edificio (coordenadas locales)
        int height;            // altura en bloques (pisos * 4)
        BlockState wall, glass, roof;
    }

    private Lot lotFor(int cx, int cz) {
        long h = cellHash(cx, cz);
        Lot l = new Lot();

        // Las 4 manzanas alrededor del origen son parques, para que el spawn sea seguro.
        boolean spawnArea = (cx == 0 || cx == -1) && (cz == 0 || cz == -1);
        int roll = (int) ((h >>> 8) & 0xFF) * 100 / 256;
        if (spawnArea || roll < 14) {
            l.park = true;
            return l;
        }

        int type = (int) ((h >>> 16) & 0xFF) * 100 / 256;
        int m0 = 1 + (int) ((h >>> 24) & 3);
        int m1 = 1 + (int) ((h >>> 26) & 3);
        int m2 = 1 + (int) ((h >>> 28) & 3);
        int m3 = 1 + (int) ((h >>> 30) & 3);
        long r = h >>> 34;
        int wi = (int) ((h >>> 40) % WALLS.length);
        int gi = (int) ((h >>> 48) % GLASSES.length);

        int floors;
        int extra = 0;
        if (type < 20) {            // rascacielos
            floors = 15 + (int) (r % 26);
            l.wall = SKY_WALLS[(int) ((h >>> 44) % SKY_WALLS.length)];
            l.glass = GLASSES[gi];
            l.roof = ROOF_FLAT;
        } else if (type < 55) {     // oficinas
            floors = 5 + (int) (r % 8);
            l.wall = WALLS[wi];
            l.glass = GLASSES[0];
            l.roof = ROOF_FLAT;
        } else if (type < 85) {     // apartamentos
            floors = 4 + (int) (r % 5);
            l.wall = WALLS[wi];
            l.glass = GLASSES[0];
            l.roof = ROOF_FLAT;
        } else {                    // casa con jardin
            floors = 1 + (int) (r % 2);
            l.wall = HOUSE_WALLS[(int) ((h >>> 44) % HOUSE_WALLS.length)];
            l.glass = GLASSES[0];
            l.roof = ROOF_HOUSE;
            l.house = true;
            extra = 4;
        }
        l.x0 = 10 + m0 + extra;
        l.x1 = 47 - m1 - extra;
        l.z0 = 10 + m2 + extra;
        l.z1 = 47 - m3 - extra;
        l.height = floors * 4;
        return l;
    }

    public Column column(int wx, int wz) {
        int cx = Math.floorDiv(wx, CELL);
        int cz = Math.floorDiv(wz, CELL);
        int lx = Math.floorMod(wx, CELL);
        int lz = Math.floorMod(wz, CELL);

        boolean roadX = lx < 10;   // calle que va de norte a sur
        boolean roadZ = lz < 10;   // calle que va de este a oeste

        if (roadX || roadZ) {
            boolean asphalt = (roadX && lx >= 2 && lx <= 7) || (roadZ && lz >= 2 && lz <= 7);
            BlockState s;
            if (asphalt) {
                s = ASPHALT;
                if (roadX && !roadZ && lx == 4 && Math.floorMod(wz, 8) < 4) s = LINE;
                else if (roadZ && !roadX && lz == 4 && Math.floorMod(wx, 8) < 4) s = LINE;
            } else {
                s = SIDEWALK;
            }
            boolean lamp = false;
            if (roadX && !roadZ && (lx == 1 || lx == 8) && Math.floorMod(wz, 16) == 0) lamp = true;
            if (roadZ && !roadX && (lz == 1 || lz == 8) && Math.floorMod(wx, 16) == 0) lamp = true;
            return new Column(s, lamp, null, lx, lz);
        }

        Lot lot = lotFor(cx, cz);
        BlockState surface;
        if (lot.park) {
            boolean path = (lx >= 28 && lx <= 29) || (lz >= 28 && lz <= 29);
            surface = path ? GRAVEL : GRASS;
        } else if (inFootprint(lot, lx, lz)) {
            surface = INTERIOR_FLOOR;
        } else {
            surface = lot.house ? GRASS : PAVING;
        }
        return new Column(surface, false, lot, lx, lz);
    }

    private static boolean inFootprint(Lot lot, int lx, int lz) {
        return lx >= lot.x0 && lx <= lot.x1 && lz >= lot.z0 && lz <= lot.z1;
    }

    /** Una columna vertical de la ciudad. */
    public static final class Column {
        private final BlockState surface;
        private final boolean lamp;
        private final Lot lot;
        private final int lx, lz;

        Column(BlockState surface, boolean lamp, Lot lot, int lx, int lz) {
            this.surface = surface;
            this.lamp = lamp;
            this.lot = lot;
            this.lx = lx;
            this.lz = lz;
        }

        /** Y mas alto que puede tener algo en esta columna. */
        public int top() {
            if (lot == null) return lamp ? GROUND + 5 : GROUND;
            if (!lot.park && inFootprint(lot, lx, lz)) return GROUND + lot.height + 2;
            return GROUND;
        }

        public BlockState at(int y) {
            if (y <= MIN_Y) return BEDROCK;
            if (y < GROUND - 7) return STONE;
            if (y < GROUND) return DIRT;
            if (y == GROUND) return surface;
            return above(y);
        }

        private BlockState above(int y) {
            if (lot == null) {
                if (!lamp) return AIR;
                int d = y - GROUND;
                if (d <= 4) return POLE;
                if (d == 5) return LANTERN;
                return AIR;
            }
            if (lot.park || !inFootprint(lot, lx, lz)) return AIR;

            int k = y - GROUND - 1;   // 0 = primer bloque sobre el suelo
            int h = lot.height;
            if (k > h + 1) return AIR;

            boolean wallCol = lx == lot.x0 || lx == lot.x1 || lz == lot.z0 || lz == lot.z1;
            boolean stair = lx == lot.x0 + 1 && lz == lot.z0 + 1;
            boolean light = Math.floorMod(lx, 6) == 0 && Math.floorMod(lz, 6) == 0;

            if (k == h + 1) {                      // parapeto de la azotea
                if (wallCol) return lot.wall;
                return stair ? LADDER : AIR;
            }
            if (k == h) {                          // techo
                if (stair) return LADDER;
                if (!wallCol && light) return LANTERN;
                return lot.roof;
            }
            if (wallCol) {
                int doorX = (lot.x0 + lot.x1) / 2;
                if (lz == lot.z1 && (lx == doorX || lx == doorX + 1) && k <= 1) return AIR;
                boolean corner = (lx == lot.x0 || lx == lot.x1) && (lz == lot.z0 || lz == lot.z1);
                int along = (lz == lot.z0 || lz == lot.z1) ? lx : lz;
                boolean window = !corner
                        && (k % 4 == 1 || k % 4 == 2)
                        && Math.floorMod(along, 3) != 0
                        && !(lx == lot.x0 + 1 && lz == lot.z0);
                return window ? lot.glass : lot.wall;
            }
            if (stair) return LADDER;
            if (k > 0 && k % 4 == 0) return light ? LANTERN : FLOOR_SLAB;
            return AIR;
        }
    }
}
