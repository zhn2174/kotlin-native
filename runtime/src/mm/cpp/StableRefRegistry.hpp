/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_STABLE_REF_REGISTRY_H
#define RUNTIME_MM_STABLE_REF_REGISTRY_H

#include "Memory.h"
#include "MultiSourceQueue.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

// Registry for all objects that have references outside of Kotlin.
class StableRefRegistry : Pinned {
public:
    class ThreadQueue : public MultiSourceQueue<ObjHeader*>::Producer {
    public:
        explicit ThreadQueue(StableRefRegistry& registry) : Producer(registry.stableRefs_) {}
        // Do not add fields as this is just a wrapper and Producer does not have virtual destructor.
    };

    using Iterable = MultiSourceQueue<ObjHeader*>::Iterable;
    using Iterator = MultiSourceQueue<ObjHeader*>::Iterator;
    using Node = MultiSourceQueue<ObjHeader*>::Node;

    static StableRefRegistry& Instance() noexcept;

    Node* RegisterStableRef(mm::ThreadData* threadData, ObjHeader* object) noexcept;

    void UnregisterStableRef(mm::ThreadData* threadData, Node* node) noexcept;

    // Collect stable references from thread corresponding to `threadData`. Must be called by the thread
    // when it's asked by GC to stop.
    void ProcessThread(mm::ThreadData* threadData) noexcept;

    // Lock registry for safe iteration.
    // TODO: Iteration over `stableRefs_` will be slow, because it's `std::list` collected at different times from
    // different threads, and so the nodes are all over the memory. Use metrics to understand how
    // much of a problem is it.
    Iterable Iter() noexcept { return stableRefs_.Iter(); }

private:
    friend class GlobalData;

    StableRefRegistry();
    ~StableRefRegistry();

    // TODO: Describe problems with the current solution and propose alternatives.
    MultiSourceQueue<ObjHeader*> stableRefs_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_STABLE_REF_REGISTRY_H
