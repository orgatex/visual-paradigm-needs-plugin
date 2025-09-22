package com.orgatex.vp.sphinx.extractor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vp.plugin.model.IUseCase;
import org.junit.jupiter.api.Test;

/** Test User ID method compatibility. */
public class UserIdDebugTest {

  @Test
  public void testUserIdMethods() throws Exception {
    // Mock a use case with User ID
    IUseCase useCase = mock(IUseCase.class);
    when(useCase.getName()).thenReturn("Test Use Case");
    when(useCase.getId()).thenReturn("VPID_123");

    // Test if getUserID method exists and can be called
    try {
      java.lang.reflect.Method getUserIdMethod = useCase.getClass().getMethod("getUserID");
      when(getUserIdMethod.invoke(useCase)).thenReturn("UC_001");

      Object result = getUserIdMethod.invoke(useCase);
      assertEquals("UC_001", result);
      System.out.println("getUserID() method works: " + result);
    } catch (NoSuchMethodException e) {
      System.out.println("getUserID() method not found");

      // Try alternative method name
      try {
        java.lang.reflect.Method getUserIdMethod = useCase.getClass().getMethod("getUserId");
        when(getUserIdMethod.invoke(useCase)).thenReturn("UC_001");

        Object result = getUserIdMethod.invoke(useCase);
        assertEquals("UC_001", result);
        System.out.println("getUserId() method works: " + result);
      } catch (NoSuchMethodException e2) {
        System.out.println("getUserId() method not found either");
      }
    }

    // Test setUserID method
    try {
      java.lang.reflect.Method setUserIdMethod =
          useCase.getClass().getMethod("setUserID", String.class);
      setUserIdMethod.invoke(useCase, "UC_001");
      System.out.println("setUserID() method works");
    } catch (NoSuchMethodException e) {
      System.out.println("setUserID() method not found");
    }
  }
}
