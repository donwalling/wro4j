package ro.isdc.wro.http.support;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.processor.ResourceProcessor;


/**
 * Test the behavior of {@link AbstractProcessorsFilter}
 * 
 * @author Alex Objelean
 */
public class TestAbstractProcessorsFilter {
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain chain;
  private ByteArrayOutputStream outputStream;
  private AbstractProcessorsFilter victim;
  
  @BeforeClass
  public static void onBeforeClass() {
    assertEquals(0, Context.countActive());
  }
  
  @AfterClass
  public static void onAfterClass() {
    assertEquals(0, Context.countActive());
  }
  
  @Before
  public void setUp()
      throws Exception {
    outputStream = new ByteArrayOutputStream();
    MockitoAnnotations.initMocks(this);
    Mockito.when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(outputStream));
  }
  
  @After
  public void tearDown() {
    Context.unset();
  }
  
  @Test
  public void shouldDoNothingWhenNoProcessorProvided() throws Exception {
    doFilterWithProcessors(Collections.<ResourceProcessor>emptyList());
    Assert.assertEquals(0, outputStream.size());
  }
  
  @Test
  public void shouldDoNothingWhenNullProcessorsProvided()
      throws Exception {
    doFilterWithProcessors(null);
    Assert.assertEquals(0, outputStream.size());
  }
  
  @Test
  public void shouldApplyProcessor()
      throws Exception {
    final String processedMessage = "DONE";
    final List<ResourceProcessor> processors = new ArrayList<ResourceProcessor>();
    processors.add(new ResourceProcessor() {
      public void process(final Resource resource, final Reader reader, final Writer writer)
          throws IOException {
        writer.write(processedMessage);
      }
    });
    doFilterWithProcessors(processors);
    Assert.assertEquals(processedMessage, new String(outputStream.toByteArray()));
  }
  
  @Test(expected = WroRuntimeException.class)
  public void shouldThrowExceptionWhenProcessorFails()
      throws Exception {
    final List<ResourceProcessor> processors = new ArrayList<ResourceProcessor>();
    processors.add(new ResourceProcessor() {
      public void process(final Resource resource, final Reader reader, final Writer writer)
          throws IOException {
        throw new WroRuntimeException("processor fails");
      }
    });
    doFilterWithProcessors(processors);
  }
  
  private void doFilterWithProcessors(final List<ResourceProcessor> processors)
      throws Exception {
    victim = new AbstractProcessorsFilter() {
      @Override
      protected List<ResourceProcessor> getProcessorsList() {
        return processors;
      }
      
      @Override
      protected void onRuntimeException(final RuntimeException e, final HttpServletResponse response,
          final FilterChain chain) {
        throw e;
      }
    };
    victim.doFilter(request, response, chain);
  }
}
