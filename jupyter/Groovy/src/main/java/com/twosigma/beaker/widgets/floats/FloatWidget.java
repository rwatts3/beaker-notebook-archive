/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beaker.widgets.floats;

import com.twosigma.beaker.widgets.DOMWidget;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class FloatWidget extends DOMWidget {

  private Double value = 0.0;

  public FloatWidget() throws NoSuchAlgorithmException {
    super();
  }

  @Override
  protected HashMap<String, Serializable> content(HashMap<String, Serializable> content) {
    super.content(content);
    content.put(VALUE, this.value);
    return content;
  }

  @Override
  protected void updateValue(Object value) {
    this.value = (value instanceof Integer) ? ((Integer) value).doubleValue() : (double) value;
  }

  public Double getValue() {
    return this.value;
  }

  public void setValue(Double value) {
    this.value = value;
    sendUpdate(VALUE, value);
  }
}