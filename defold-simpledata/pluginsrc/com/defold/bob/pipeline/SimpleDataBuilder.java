// Copyright 2020 The Defold Foundation
// Licensed under the Defold License version 1.0 (the "License"); you may not use
// this file except in compliance with the License.
//
// You may obtain a copy of the License, together with FAQs at
// https://www.defold.com/license
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.dynamo.bob.pipeline;

import com.dynamo.bob.BuilderParams;
import com.dynamo.bob.CompileExceptionError;
import com.dynamo.bob.ProtoBuilder;
import com.dynamo.bob.ProtoParams;
import com.dynamo.bob.Task;
import com.dynamo.bob.fs.IResource;
import com.dynamo.bob.pipeline.BuilderUtil;
import com.dynamo.simpledata.proto.SimpleData.SimpleDataDesc;

@ProtoParams(srcClass = SimpleDataDesc.class, messageClass = SimpleDataDesc.class)
@BuilderParams(name="SimpleData", inExts=".simpledata", outExt=".simpledatac")
public class SimpleDataBuilder extends ProtoBuilder<SimpleDataDesc.Builder> {

    @Override
    protected SimpleDataDesc.Builder transform(Task<Void> task, IResource resource, SimpleDataDesc.Builder builder) throws CompileExceptionError {
        // Add any transforms here
        return builder;
    }
}
