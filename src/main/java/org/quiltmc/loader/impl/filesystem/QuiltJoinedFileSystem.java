package org.quiltmc.loader.impl.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/** A {@link FileSystem} that exposes multiple {@link Path}s in a single */
public class QuiltJoinedFileSystem extends QuiltBaseFileSystem<QuiltJoinedFileSystem, QuiltJoinedPath> {

	final Path[] from;
	final boolean[] shouldCloseFroms;
	boolean isOpen = true;

	public QuiltJoinedFileSystem(String name, List<Path> from) {
		this(name, from, null);
	}

	public QuiltJoinedFileSystem(String name, List<Path> from, List<Boolean> shouldClose) {
		super(QuiltJoinedFileSystem.class, QuiltJoinedPath.class, name);
		this.from = from.toArray(new Path[0]);
		this.shouldCloseFroms = new boolean[from.size()];
		for (int i = 0; i < shouldCloseFroms.length; i++) {
			shouldCloseFroms[i] = shouldClose != null && shouldClose.get(i);
		}
		QuiltJoinedFileSystemProvider.register(this);
	}

	@Override
	QuiltJoinedPath createPath(@Nullable QuiltJoinedPath parent, String name) {
		return new QuiltJoinedPath(this, parent, name);
	}

	@Override
	public FileSystemProvider provider() {
		return QuiltJoinedFileSystemProvider.instance();
	}

	@Override
	public synchronized void close() throws IOException {
		if (isOpen) {
			isOpen = false;
			QuiltJoinedFileSystemProvider.closeFileSystem(this);
			for (int i = 0; i < from.length; i++) {
				if (shouldCloseFroms[i]) {
					from[i].getFileSystem().close();
				}
			}
		}
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		// TODO Auto-generated method stub
		throw new AbstractMethodError("// TODO: Implement this!");
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		Set<String> supported = new HashSet<>();
		for (Path path : from) {
			Set<String> set = path.getFileSystem().supportedFileAttributeViews();
			if (supported.isEmpty()) {
				supported.addAll(set);
			} else {
				supported.retainAll(set);
			}
		}
		return supported;
	}

	public int getBackingPathCount() {
		return from.length;
	}

	public Path getBackingPath(int index, QuiltJoinedPath thisPath) {
		Path other = from[index];
		if (getSeparator().equals(other.getFileSystem().getSeparator())) {
			return other.resolve(thisPath.toString());
		} else {
			for (String segment : thisPath.names()) {
				other = other.resolve(segment);
			}
			return other;
		}
	}
}