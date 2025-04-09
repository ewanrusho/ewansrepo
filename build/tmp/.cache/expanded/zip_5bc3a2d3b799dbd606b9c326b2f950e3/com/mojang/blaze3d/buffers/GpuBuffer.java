package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuBuffer implements AutoCloseable {
    private static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
    private final BufferType type;
    private final BufferUsage usage;
    private boolean closed;
    private boolean initialized = false;
    public final int handle;
    public int size;

    public GpuBuffer(BufferType pType, BufferUsage pUsage, int pSize) {
        this.type = pType;
        this.size = pSize;
        this.usage = pUsage;
        this.handle = GlStateManager._glGenBuffers();
    }

    public GpuBuffer(BufferType pType, BufferUsage pUsage, ByteBuffer pBuffer) {
        this(pType, pUsage, pBuffer.remaining());
        this.write(pBuffer, 0);
    }

    public void resize(int pSize) {
        if (this.closed) {
            throw new IllegalStateException("Buffer already closed");
        } else {
            if (this.initialized) {
                MEMORY_POOl.free((long)this.handle);
            }

            this.size = pSize;
            if (this.usage.writable) {
                this.initialized = false;
            } else {
                this.bind();
                GlStateManager._glBufferData(this.type.id, (long)pSize, this.usage.id);
                MEMORY_POOl.malloc((long)this.handle, pSize);
                this.initialized = true;
            }
        }
    }

    public void write(ByteBuffer pBuffer, int pOffset) {
        if (this.closed) {
            throw new IllegalStateException("Buffer already closed");
        } else if (!this.usage.writable) {
            throw new IllegalStateException("Buffer is not writable");
        } else {
            int i = pBuffer.remaining();
            if (i + pOffset > this.size) {
                throw new IllegalArgumentException(
                    "Cannot write more data than this buffer can hold (attempting to write "
                        + i
                        + " bytes at offset "
                        + pOffset
                        + " to "
                        + this.size
                        + " size buffer)"
                );
            } else {
                this.bind();
                if (this.initialized) {
                    GlStateManager._glBufferSubData(this.type.id, pOffset, pBuffer);
                } else if (pOffset == 0 && i == this.size) {
                    GlStateManager._glBufferData(this.type.id, pBuffer, this.usage.id);
                    MEMORY_POOl.malloc((long)this.handle, this.size);
                    this.initialized = true;
                } else {
                    GlStateManager._glBufferData(this.type.id, (long)this.size, this.usage.id);
                    GlStateManager._glBufferSubData(this.type.id, pOffset, pBuffer);
                    MEMORY_POOl.malloc((long)this.handle, this.size);
                    this.initialized = true;
                }
            }
        }
    }

    @Nullable
    public GpuBuffer.ReadView read() {
        return this.read(0, this.size);
    }

    @Nullable
    public GpuBuffer.ReadView read(int pOffset, int pLength) {
        if (this.closed) {
            throw new IllegalStateException("Buffer already closed");
        } else if (!this.usage.readable) {
            throw new IllegalStateException("Buffer is not readable");
        } else if (pOffset + pLength > this.size) {
            throw new IllegalArgumentException(
                "Cannot read more data than this buffer can hold (attempting to read "
                    + pLength
                    + " bytes at offset "
                    + pOffset
                    + " from "
                    + this.size
                    + " size buffer)"
            );
        } else {
            this.bind();
            ByteBuffer bytebuffer = GlStateManager._glMapBufferRange(this.type.id, pOffset, pLength, 1);
            return bytebuffer == null ? null : new GpuBuffer.ReadView(this.type.id, bytebuffer);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            GlStateManager._glDeleteBuffers(this.handle);
            if (this.initialized) {
                MEMORY_POOl.free((long)this.handle);
            }
        }
    }

    public void bind() {
        GlStateManager._glBindBuffer(this.type.id, this.handle);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ReadView implements AutoCloseable {
        private final int target;
        private final ByteBuffer data;

        protected ReadView(int pTarget, ByteBuffer pData) {
            this.target = pTarget;
            this.data = pData;
        }

        public ByteBuffer data() {
            return this.data;
        }

        @Override
        public void close() {
            GlStateManager._glUnmapBuffer(this.target);
        }
    }
}