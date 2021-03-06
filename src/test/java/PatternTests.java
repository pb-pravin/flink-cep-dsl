import at.datasciencelabs.pattern.Event;
import at.datasciencelabs.pattern.Pattern;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.shaded.curator.org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PatternTests {

    private static Map<String, List<Event>> results = new HashMap<>();

    @Before
    public void onBefore() {
        results.clear();
    }

    @Test
    public void shouldEvaluateNextPattern() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);
        executeTest("A(attribute='testabc') B(attribute=30)", Lists.newArrayList(event, event2));

        assertThat(results.size(), is(2));
    }

    @Test
    public void shouldEvaluateNextPatternAndFail() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", "testabc2");
        TestEvent event3 = new TestEvent();
        event3.setAttribute("attribute", 30);
        executeTest("A(attribute='testabc') B(attribute=30)", Lists.newArrayList(event, event2, event3));

        assertThat(results.size(), is(0));
    }

    @Test
    public void shouldEvaluateFollowedByPattern() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", "testabc2");
        TestEvent event3 = new TestEvent();
        event3.setAttribute("attribute", 30);
        executeTest("A(attribute='testabc') -> B(attribute=30)", Lists.newArrayList(event, event2, event3));

        assertThat(results.size(), is(2));
    }

    @Test
    public void shouldEvaluateTimes4() throws Exception {
        List<Event> events = generate(4);

        executeTest("A{4}(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(4));
    }

    @Test
    public void shouldEvaluateTimes4OrZero() throws Exception {
        List<Event> events = generate(4);

        executeTest("A{4}(attribute='testabc')?", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(4));
    }

    @Test
    public void shouldEvaluateThreeForTwoThreeOrFour() throws Exception {
        List<Event> events = generate(3);

        executeTest("A{2,4}(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        // all results are 2 because it is not greedy
        assertThat(results.get("A").size(), is(2));
    }

    @Test
    public void shouldEvaluateTwoForTwoThreeOrFour() throws Exception {
        List<Event> events = generate(2);

        executeTest("A{2,4}(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        // all results are 2 because it is not greedy
        assertThat(results.get("A").size(), is(2));
    }

    @Test
    public void shouldEvaluateFourForTwoThreeOrFour() throws Exception {
        List<Event> events = generate(4);

        executeTest("A{2,4}(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        // all results are 2 because it is not greedy
        assertThat(results.get("A").size(), is(2));
    }

    @Test
    public void shouldEvaluateFourForTwoThreeOrFourGreedy() throws Exception {
        List<Event> events = generate(4);

        executeTest("A{2,4}?(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(4));
    }

    @Test
    public void shouldEvaluateZeroOrMoreOne() throws Exception {
        List<Event> events = generate(1);

        executeTest("A*(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluteZeroOrMoreTwoTestEvents() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);

        executeTest("A*(attribute='testabc') B(attribute=30)", Lists.newArrayList(event, event2));

        assertThat(results.size(), is(2));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluteZeroOrMoreOneTestEvent() throws Exception {
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);

        executeTest("A*(attribute='testabc') B(attribute=30)", Lists.newArrayList(event2));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
        assertThat(results.containsKey("A"), is(false));
    }

    @Test
    public void shouldEvaluteOneOptionalOneTestEvent() throws Exception {
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);

        executeTest("A{1}(attribute='testabc')? B(attribute=30)", Lists.newArrayList(event2));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
        assertThat(results.containsKey("A"), is(false));
    }

    @Test
    public void shouldEvaluteTwoOptionalOneTestEvent() throws Exception {
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);

        executeTest("A{2}(attribute='testabc')? B(attribute=30)", Lists.newArrayList(event2));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
        assertThat(results.containsKey("A"), is(false));
    }

    @Test
    public void shouldEvaluateZeroOrMoreTwo() throws Exception {
        List<Event> events = generate(2);

        executeTest("A*(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        // all results are 2 because it is not greedy
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluateTimesOrMoreThree() throws Exception {
        List<Event> events = generate(3);

        executeTest("A{2,+}(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        // all results are 2 because it is not greedy
        assertThat(results.get("A").size(), is(2));
    }

    @Test
    public void shouldEvaluateTimesOrMoreThreeGreedy() throws Exception {
        List<Event> events = generate(3);

        executeTest("A{2,+}?(attribute='testabc')", events);

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(3));
    }

    @Test
    public void shouldEvaluateTimesOrMoreThreeGreedyOptional() throws Exception {
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", 30);

        executeTest("A{2,+}?(attribute='testabc')? B(attribute=30)", Lists.newArrayList(event2));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(false));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
    }

    @Test
    public void shouldEvaluateFollowedByAny() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", "testabc2");
        TestEvent event3 = new TestEvent();
        event3.setAttribute("attribute", 30);

        executeTest("A(attribute='testabc') ->> B(attribute=30)", Lists.newArrayList(event, event2, event3));
    }

    @Test
    public void shouldEvaluateCorrelation() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        event.setAttribute("correlation_id", 10);
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", "testabc2");
        event2.setAttribute("correlation_id", 10);

        executeTest("A(attribute='testabc') -> B(attribute='testabc2' and correlation_id=A.correlation_id)", Lists.newArrayList(event, event2));

        assertThat(results.size(), is(2));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
    }

    @Test
    public void shouldEvaluateAndExpression() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", "testabc");
        event.setAttribute("correlation_id", 10);
        TestEvent event2 = new TestEvent();
        event2.setAttribute("attribute", "testabc2");
        event2.setAttribute("correlation_id", 10);

        executeTest("A(attribute='testabc') -> B(attribute='testabc2' and correlation_id=10)", Lists.newArrayList(event, event2));

        assertThat(results.size(), is(2));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
        assertThat(results.containsKey("B"), is(true));
        assertThat(results.get("B").size(), is(1));
    }

    @Test
    public void shouldEvaluateGreaterThan() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", 20);

        executeTest("A(attribute > 10)", Lists.newArrayList(event));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluateLowerThan() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", 5);

        executeTest("A(attribute < 10)", Lists.newArrayList(event));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluateLowerThanEquals() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", 10);

        executeTest("A(attribute <= 10)", Lists.newArrayList(event));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluateGreaterThanEquals() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", 10);

        executeTest("A(attribute >= 10)", Lists.newArrayList(event));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    @Test
    public void shouldEvaluateNotEquals() throws Exception {
        TestEvent event = new TestEvent();
        event.setAttribute("attribute", 20);

        executeTest("A(attribute != 10)", Lists.newArrayList(event));

        assertThat(results.size(), is(1));
        assertThat(results.containsKey("A"), is(true));
        assertThat(results.get("A").size(), is(1));
    }

    private List<Event> generate(int amount) {
        List<Event> events = new ArrayList<>();
        for(int i = 0;  i < amount; i++) {
            TestEvent event = new TestEvent();
            event.setAttribute("attribute", "testabc");
            events.add(event);
        }
        return events;
    }

    private void executeTest(String pattern, List<Event> data) throws Exception {

        StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<Event> eventDataStream = streamExecutionEnvironment.fromCollection(data);

        PatternStream<Event> patternStream = Pattern.transform(pattern, eventDataStream);

        patternStream.select(new PatternSelectFunction<Event, Event>() {
            @Override
            public Event select(Map<String, List<Event>> map) throws Exception {
                results.putAll(map);
                return null;
            }
        });

        streamExecutionEnvironment.execute("test");
    }
}
