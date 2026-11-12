package dev.modforge.railpalette.data;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class PaintJob {
    public final UUID owner;
    public final int dye;
    public final ArrayDeque<RailNode> frontier = new ArrayDeque<>();
    public final Set<RailNode> discovered = new HashSet<>();
    public long painted;

    public PaintJob(UUID owner, int dye, RailNode seed) {
        this.owner = owner;
        this.dye = dye;
        offer(seed);
    }

    private PaintJob(UUID owner, int dye) {
        this.owner = owner;
        this.dye = dye;
    }

    public void offer(RailNode node) {
        if (discovered.add(node)) {
            frontier.addLast(node);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", owner);
        tag.putInt("Dye", dye);
        tag.putLong("Painted", painted);
        ListTag queue = new ListTag();
        frontier.forEach(node -> queue.add(node.save()));
        tag.put("Frontier", queue);
        ListTag seen = new ListTag();
        discovered.forEach(node -> seen.add(node.save()));
        tag.put("Discovered", seen);
        return tag;
    }

    public static PaintJob load(CompoundTag tag) {
        PaintJob job = new PaintJob(tag.getUUID("Owner"), Math.clamp(tag.getInt("Dye"), 0, 15));
        job.painted = tag.getLong("Painted");
        ListTag queue = tag.getList("Frontier", Tag.TAG_COMPOUND);
        for (int i = 0; i < queue.size(); i++) {
            job.frontier.addLast(RailNode.load(queue.getCompound(i)));
        }
        ListTag seen = tag.getList("Discovered", Tag.TAG_COMPOUND);
        for (int i = 0; i < seen.size(); i++) {
            job.discovered.add(RailNode.load(seen.getCompound(i)));
        }
        job.discovered.addAll(job.frontier);
        return job;
    }
}
