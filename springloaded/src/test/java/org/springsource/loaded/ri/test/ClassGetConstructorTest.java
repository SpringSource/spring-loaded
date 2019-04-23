/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded.ri.test;

import static org.springsource.loaded.ri.test.AbstractReflectionTests.newInstance;
import static org.springsource.loaded.test.SpringLoadedTests.runOnInstance;

import java.util.HashSet;
import java.util.Set;

import org.junit.runner.RunWith;
import org.objectweb.asm.Type;
import org.springsource.loaded.test.infra.Result;
import org.springsource.loaded.test.infra.ResultException;
import org.springsource.loaded.testgen.ExploreAllChoicesRunner;
import org.springsource.loaded.testgen.GenerativeSpringLoadedTest;
import org.springsource.loaded.testgen.RejectedChoice;
import org.springsource.loaded.testgen.SignatureFinder;


/**
 * Tests for Class.getConstructor Class.getDeclaredConstructor
 * 
 * @author kdvolder
 */
@RunWith(ExploreAllChoicesRunner.class)
// @PredictResult
public class ClassGetConstructorTest extends GenerativeSpringLoadedTest {

	private static final String TARGET_PACKAGE = "reflection.constructors";

	/**
	 * Cached list of available constructor signatures. This doesn't need to be rediscovered for each test run since it
	 * is not expected to change.
	 */
	private static String[] methodSignatureCache = null;

	/**
	 * List of target type names used by signature finder
	 */
	private static String[] targetTypeNames = { TARGET_PACKAGE + "." + "ClassWithConstructors" };

	// Needed to run the tests (non-changing parameters)
	private Class<?> callerClazz;

	private Object callerInstance;

	// Parameters that change for different test runs
	private Class<?> targetClass; //One class chosen to focus test on

	private String targetMethodName;

	private Class<?>[] params;

	private String methodDescriptor;

	@Override
	protected String getTargetPackage() {
		return TARGET_PACKAGE;
	}

	@Override
	protected void chooseTestParameters() throws RejectedChoice, Exception {
		targetMethodName = "call" + choice("GetConstructor", "GetDeclaredConstructor");
		toStringValue.append(targetMethodName + ": ");

		if (choice()) {
			//Try a non reloadable class
			targetClass = targetClass("java.lang.Object");
		}
		else {
			//Will focus on the 'ClassTarget' only 'ClassTarget' needs to be loaded
			targetClass = targetClass("ClassWithConstructors", choice("", "002"));
		}

		callerClazz = loadClassVersion("reflection.ClassInvoker", "");
		callerInstance = newInstance(callerClazz);

		chooseMethodSignature();

		toStringValue.append("." + methodDescriptor);
	}

	private void chooseMethodSignature() throws Exception {
		methodDescriptor = choice(getMethodSignatures());
		Type[] asmTypes = Type.getArgumentTypes(methodDescriptor);
		params = new Class<?>[asmTypes.length];
		//Get the corresponding class for each param type name
		for (int i = 0; i < asmTypes.length; i++) {
			params[i] = classForName(asmTypes[i].getClassName());
		}

	}

	private String[] getMethodSignatures() throws Exception {
		if (methodSignatureCache == null) {
			SignatureFinder sigFinder = new SignatureFinder();
			Set<String> sigs = new HashSet<String>();
			for (String targetType : targetTypeNames) {
				sigFinder.gatherConstructorSignatures(targetType, sigs);
			}
			methodSignatureCache = sigs.toArray(new String[sigs.size()]);
		}
		return methodSignatureCache;
	}

	@Override
	public Result test() throws ResultException, Exception {
		Result r = runOnInstance(callerClazz, callerInstance, targetMethodName, targetClass, params);
		return r;
	}

}
