/*
 * This file heavily references a file from the MIT licensed fabric-loom.
 *
 * Copyright (c) 2019-2025 albion, FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.zomboiddecompiler;

import org.objectweb.asm.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class responsible for remapping java class lines to new source lines.
 */
public final class LineRemapper {
    /**
     * Remaps a class file's line numbers.
     * @param file The path of the class file to remap.
     * @param destination The path to write the remapped file to. It may be the same path as <code>file</code>.
     * @param mappings Mappings of bytecode line numbers to source line numbers.
     */
    public static void remapClass(Path file, Path destination, Map<Integer, Integer> mappings) {
        remapClass(file, destination, mappings, true, false);
    }

    /**
     * Remaps a class file's line numbers with optional controls.
     * @param file The path of the class file to remap.
     * @param destination The path to write the remapped file to. It may be the same path as <code>file</code>.
     * @param mappings Mappings of bytecode line numbers to source line numbers.
     * @param writeBackup If true, writes a .backup copy before overwriting.
     * @param computeFrames If true, asks ASM to compute frames/maxs for resilience.
     */
    public static void remapClass(Path file, Path destination, Map<Integer, Integer> mappings, boolean writeBackup, boolean computeFrames) {
        assert Files.exists(file) && Files.isRegularFile(file);

        try (var in = Files.newInputStream(file)) {
            // Read original bytecode
            ClassReader reader = new ClassReader(in);
            int writerFlags = computeFrames ? (ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) : 0;
            ClassWriter writer = new ClassWriter(writerFlags);
            reader.accept(
                    new ClassLineRemapper(
                            Opcodes.ASM9,
                            writer,
                            new LineNumbers(mappings)),
                    Opcodes.ASM9);

            // Backup original before writing (optional)
            if (writeBackup) {
                Files.copy(file, destination.resolveSibling(file.getFileName().toString() + ".backup"), StandardCopyOption.REPLACE_EXISTING);
            }

            // Write remapped class
            Files.write(destination, writer.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class LineNumbers {
        private final Map<Integer, Integer> mappings;
        /// Highest key; null indicates an empty mapping.
        private Integer max;

        /**
         * Gets the corresponding source line number for a bytecode line number.
         * @param line The bytecode line number.
         * @return The corresponding source line number.
         */
        Integer getSourceLineNumber(Integer line) {
            if (line == 0) {
                return 0;
            }

            // No mappings available: keep original line numbers to avoid NPEs.
            if (max == null) {
                return line;
            }

            if (line >= max) {
                return mappings.getOrDefault(max, line);
            }

            int cursor = line;
            while (cursor < max) {
                Integer sourceLine = mappings.get(cursor);
                if (sourceLine != null) {
                    return sourceLine;
                }
                cursor++;
            }

            // Fallback: return the original line when no mapping was found
            return line;
        }

        public LineNumbers(Map<Integer, Integer> mappings) {
            this.mappings = mappings;
            max = null;
            for (Integer key: mappings.keySet()) {
                if (max == null || key > max) {
                    max = key;
                }
            }
        }

        public LineNumbers() {
            this(new LinkedHashMap<>());
        }
    }

    private static final class MethodLineRemapper extends MethodVisitor {
        private final LineNumbers lineNumbers;

        private MethodLineRemapper(int api, MethodVisitor methodVisitor, LineNumbers lineNumbers) {
            super(api, methodVisitor);
            this.lineNumbers = lineNumbers;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            // pass the 'corrected' line number to the attached methodvisitor (usually a ClassWriter)
            super.visitLineNumber(lineNumbers.getSourceLineNumber(line), start);
        }
    }

    private static final class ClassLineRemapper extends ClassVisitor {
        LineNumbers mappings;

        private ClassLineRemapper(int api, ClassVisitor classVisitor, LineNumbers mappings) {
            super(api, classVisitor);
            this.mappings = mappings;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodLineRemapper(
                    api,
                    super.visitMethod(access, name, descriptor, signature, exceptions),
                    mappings
            );
        }
    }
}
