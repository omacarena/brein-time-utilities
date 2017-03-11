package com.brein.time.timeintervals.indexes;

import com.brein.time.timeintervals.collections.IntervalCollection;
import com.brein.time.timeintervals.collections.IntervalCollection.IntervalFilter;
import com.brein.time.timeintervals.collections.IntervalCollection.IntervalFilters;
import com.brein.time.timeintervals.intervals.Interval;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class IntervalTreeNode extends IntervalTreeNodeContext
        implements Iterable<Interval>, Comparable<IntervalTreeNode> {
    private static final Logger LOGGER = Logger.getLogger(IntervalTreeNode.class);

    private final IntervalCollection collection;
    private final long start;
    private final long end;

    private long max;
    private long level;
    private long height;

    public IntervalTreeNode(final Interval interval,
                            final IntervalCollection collection) {
        this.start = interval.getNormStart();
        this.end = interval.getNormEnd();
        this.max = interval.getNormEnd();
        this.level = 0L;
        this.height = 1L;

        this.collection = collection;

        if (!this.collection.isEmpty()) {
            LOGGER.warn("New IntervalTreeNode with filled collection, collection will be cleared: " + collection);
            this.collection.clear();
        }

        this.collection.add(interval);
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getMax() {
        return max;
    }

    public void setMax(final long max) {
        if (this.max == max) {
            return;
        }
        this.max = max;

        if (hasParent()) {
            getParent().updateMax();
        }
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(final long level) {
        if (this.level == level) {
            return;
        }
        this.level = level;

        if (hasLeft()) {
            getLeft().setLevel(level + 1);
        }
        if (hasRight()) {
            getRight().setLevel(level + 1);
        }
    }

    public void updateMax() {
        if (isLeaf()) {
            setMax(this.end);
        } else if (isSingleParent()) {
            setMax(Math.max(this.end, getSingleChild().getMax()));
        } else {
            setMax(Math.max(getLeft().getMax(), getRight().getMax()));
        }
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(final long height) {
        if (this.height == height) {
            return;
        }
        this.height = height;

        if (hasParent()) {
            getParent().updateHeight();
        }
    }

    public void updateHeight() {
        if (isLeaf()) {
            setHeight(1L);
        } else if (isSingleParent()) {
            setHeight(getSingleChild().getHeight() + 1);
        } else {
            setHeight(Math.max(getLeft().getHeight(), getRight().getHeight()) + 1);
        }
    }

    public Collection<Interval> getIntervals() {
        return Collections.unmodifiableCollection(collection);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean addInterval(final Interval interval) {
        if (interval.getNormStart() == this.start &&
                interval.getNormEnd() == this.end) {
            return this.collection.add(interval);
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return collection.isEmpty();
    }

    public boolean removeInterval(final Interval interval) {
        return this.collection.remove(interval);
    }

    public Collection<Interval> find(final Interval interval) {
        return find(interval, IntervalFilters.EQUAL);
    }

    public Collection<Interval> find(final Interval interval, final IntervalFilter filter) {
        return this.collection.find(interval, filter);
    }

    public String getId() {
        return String.format("[%d, %d]", this.start, this.end);
    }

    @Override
    public String toString() {
        return String.format("[%d, %d] (max: %d, count: %d, level: %d, height: %d)",
                this.start, this.end, this.max, this.collection.size(), this.level, this.height);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int compareTo(final IntervalTreeNode node) {
        return compareTo(node.start, node.end);
    }

    public int compareTo(final Interval node) {
        return compareTo(node.getNormStart(), node.getNormEnd());
    }

    public int compareTo(final long start, final long end) {
        if (this.start < start) {
            return -1;
        } else if (this.start == start) {
            if (this.end == end) {
                return 0;
            } else if (this.end < end) {
                return -1;
            } else {
                return 1;
            }
        } else {
            return 1;
        }
    }

    @Override
    public void setLeft(final IntervalTreeNode left) {
        setChild(left, IntervalTreeNodeChildType.LEFT);
    }

    @Override
    public void setRight(final IntervalTreeNode right) {
        setChild(right, IntervalTreeNodeChildType.RIGHT);
    }

    protected void setChild(final IntervalTreeNode node, final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            super.setLeft(node);
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            super.setRight(node);
        }

        // set the new parent
        if (node != null) {
            node.setParent(this);
            node.setLevel(this.level + 1);
        }

        updateMax();
        updateHeight();
    }

    protected IntervalTreeNode get(final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            return getLeft();
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            return getRight();
        } else {
            return null;
        }
    }

    public IntervalTreeNodeContext detach() {
        final IntervalTreeNodeContext ctx = new IntervalTreeNodeContext(this);

        // remove it from the parent
        final IntervalTreeNode parent = this.getParent();
        if (parent != null) {
            parent.removeChild(this);
        }

        this.setParent(null);
        this.setLeft(null);
        this.setRight(null);
        this.max = this.getEnd();
        this.level = 0L;

        return ctx;
    }

    public void removeChild(final IntervalTreeNode node) {
        replaceChild(node, IntervalTreeNode.class.cast(null));
    }

    public void replaceChild(final IntervalTreeNode replacement, final IntervalTreeNodeChildType childType) {
        if (IntervalTreeNodeChildType.LEFT.equals(childType)) {
            setLeft(replacement);
        } else if (IntervalTreeNodeChildType.RIGHT.equals(childType)) {
            setRight(replacement);
        }
    }

    public void replaceChild(final IntervalTreeNode replacee, final IntervalTreeNode replacement) {
        if (getLeft() == replacee) {
            setLeft(replacement);
        } else if (getRight() == replacee) {
            setRight(replacement);
        }
    }

    @Override
    public Iterator<Interval> iterator() {
        return this.collection.iterator();
    }

    public IntervalTreeNodeChildType determineChildType() {
        if (!hasParent()) {
            return IntervalTreeNodeChildType.NONE;
        } else if (getParent().getLeft() == this) {
            return IntervalTreeNodeChildType.LEFT;
        } else if (getParent().getRight() == this) {
            return IntervalTreeNodeChildType.RIGHT;
        } else {
            return IntervalTreeNodeChildType.NONE;
        }
    }
}