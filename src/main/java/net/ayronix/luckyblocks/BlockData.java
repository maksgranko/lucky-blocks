package net.ayronix.luckyblocks;

public class BlockData
{
    public final int x;
    public final int y;
    public final int z;
    public final String type;
    public final int level;

    public BlockData(int x, int y, int z, String type, int level)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.level = level;
    }
}
