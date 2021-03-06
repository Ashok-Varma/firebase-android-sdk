// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.functions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.google.firebase.FirebaseApp;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FirebaseFunctionsTest {
  private final FirebaseApp app = mock(FirebaseApp.class);
  private final ContextProvider provider = mock(ContextProvider.class);

  @Before
  public void setUp() {
    when(app.get(FunctionsMultiResourceComponent.class))
        .thenReturn(
            new FunctionsMultiResourceComponent(
                InstrumentationRegistry.getTargetContext(), provider, "my-project"));
  }

  @Test
  public void testGetUrl() {
    FirebaseFunctions functions = FirebaseFunctions.getInstance(app, "my-region");
    URL url = functions.getURL("my-endpoint");
    assertEquals("https://my-region-my-project.cloudfunctions.net/my-endpoint", url.toString());

    functions = FirebaseFunctions.getInstance(app);
    url = functions.getURL("my-endpoint");
    assertEquals("https://us-central1-my-project.cloudfunctions.net/my-endpoint", url.toString());
  }
}
