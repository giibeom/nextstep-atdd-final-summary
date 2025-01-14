package nextstep.subway.acceptance;

import static nextstep.subway.acceptance.support.CommonSupporter.조회에_성공한다;
import static nextstep.subway.acceptance.support.LineSteps.역이_순서대로_정렬되어_있다;
import static nextstep.subway.acceptance.support.LineSteps.지하철_노선_생성_요청;
import static nextstep.subway.acceptance.support.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.support.PathSteps.로그인_후_지하철_경로_조회_요청;
import static nextstep.subway.acceptance.support.PathSteps.지하철_경로_조회_요청;
import static nextstep.subway.acceptance.support.PathSteps.총_거리와_소요_시간이_조회된다;
import static nextstep.subway.acceptance.support.PathSteps.총_이용_요금이_조회된다;
import static nextstep.subway.acceptance.support.StationSteps.지하철역_생성_요청;
import static nextstep.subway.domain.path.PathType.DISTANCE;
import static nextstep.subway.domain.path.PathType.DURATION;
import static nextstep.subway.fixture.LineFixture.삼호선;
import static nextstep.subway.fixture.LineFixture.신분당선;
import static nextstep.subway.fixture.LineFixture.이호선;
import static nextstep.subway.fixture.MemberFixture.어린이_회원_BEOM;
import static nextstep.subway.fixture.MemberFixture.청소년_회원_JADE;
import static nextstep.subway.fixture.SectionFixture.강남_양재_구간;
import static nextstep.subway.fixture.SectionFixture.교대_강남_구간;
import static nextstep.subway.fixture.SectionFixture.교대_남부터미널_구간;
import static nextstep.subway.fixture.SectionFixture.남부터미널_양재_구간;
import static nextstep.subway.fixture.StationFixture.강남역;
import static nextstep.subway.fixture.StationFixture.교대역;
import static nextstep.subway.fixture.StationFixture.남부터미널역;
import static nextstep.subway.fixture.StationFixture.양재역;
import static nextstep.subway.utils.JsonPathUtil.식별자_ID_추출;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.acceptance.support.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("지하철 경로 조회 기능 인수 테스트")
class PathAcceptanceTest extends AcceptanceTest {
    private Long 교대역_ID;
    private Long 강남역_ID;
    private Long 양재역_ID;
    private Long 남부터미널역_ID;
    private Long 이호선_ID;
    private Long 신분당선_ID;
    private Long 삼호선_ID;


    /** (구간 거리, 소요 시간)
     *  신분당선 소요시간 = 구간거리 / 2  |  3호선 소요시간 = 구간거리 * 2
     *
     *   교대역     --- (6, 12) --- 강남역
     *     |                        |
     *   (3, 6)                  (10, 5)
     *   *3호선*                  *신분당선*
     *     |                        |
     * 남부터미널역  --- (7, 14) --- 양재역
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역_ID = 식별자_ID_추출(지하철역_생성_요청(교대역));
        강남역_ID = 식별자_ID_추출(지하철역_생성_요청(강남역));
        양재역_ID = 식별자_ID_추출(지하철역_생성_요청(양재역));
        남부터미널역_ID = 식별자_ID_추출(지하철역_생성_요청(남부터미널역));

        이호선_ID = 식별자_ID_추출(지하철_노선_생성_요청(이호선.등록_요청_데이터_생성(교대역_ID, 강남역_ID, 교대_강남_구간)));
        신분당선_ID = 식별자_ID_추출(지하철_노선_생성_요청(신분당선.등록_요청_데이터_생성(강남역_ID, 양재역_ID, 강남_양재_구간)));
        삼호선_ID = 식별자_ID_추출(지하철_노선_생성_요청(삼호선.등록_요청_데이터_생성(교대역_ID, 남부터미널역_ID, 교대_남부터미널_구간)));

        지하철_노선에_지하철_구간_생성_요청(삼호선_ID, 남부터미널_양재_구간.요청_데이터_생성(남부터미널역_ID, 양재역_ID));
    }

    /**
     * When 교대역에서 양재역까지의 최단 거리 경로를 조회하면
     * Then 교대역 -> 남부터미널역 -> 양재역 순으로 역 목록이 조회된다
     * And 지하철 총 이용 요금이 조회된다 (1250원)
     */
    @DisplayName("두 역의 최단 거리 경로를 조회한다")
    @Test
    void findPathByDistance() {
        // when
        ExtractableResponse<Response> 경로_조회_결과 = 지하철_경로_조회_요청(교대역_ID, 양재역_ID, DISTANCE.name());

        // then
        조회에_성공한다(경로_조회_결과);
        역이_순서대로_정렬되어_있다(경로_조회_결과, 교대역_ID, 남부터미널역_ID, 양재역_ID);
        총_거리와_소요_시간이_조회된다(
                경로_조회_결과,
                교대_남부터미널_구간.구간_거리() + 남부터미널_양재_구간.구간_거리(),
                교대_남부터미널_구간.구간_소요시간() + 남부터미널_양재_구간.구간_소요시간()
        );
        총_이용_요금이_조회된다(경로_조회_결과, 1250);
    }

    /**
     * When 교대역에서 양재역까지의 최소 시간 경로를 조회하면
     * Then 교대역 -> 강남역 -> 양재역 순으로 역 목록이 조회된다
     * And 지하철 총 이용 요금이 조회된다 (1250 + 900(신분당선 추가요금) + 200 = 2350원)
     */
    @DisplayName("두 역의 최소 시간 경로를 조회한다")
    @Test
    void findPathByDuration() {
        // when
        ExtractableResponse<Response> 경로_조회_결과 = 지하철_경로_조회_요청(교대역_ID, 양재역_ID, DURATION.name());

        // then
        조회에_성공한다(경로_조회_결과);
        역이_순서대로_정렬되어_있다(경로_조회_결과, 교대역_ID, 강남역_ID, 양재역_ID);
        총_거리와_소요_시간이_조회된다(
                경로_조회_결과,
                교대_강남_구간.구간_거리() + 강남_양재_구간.구간_거리(),
                교대_강남_구간.구간_소요시간() + 강남_양재_구간.구간_소요시간()
        );
        총_이용_요금이_조회된다(경로_조회_결과, 2350);
    }

    /**
     * When  청소년 계정으로 교대역에서 양재역까지의 최소 시간 경로를 조회하면
     * Then 교대역 -> 강남역 -> 양재역 순으로 역 목록이 조회된다
     * And 청소년 요금이 할인된 지하철 총 이용 요금이 조회된다 ((1250 + 900 + 200 - 350) x 0.8 = 1600원)
     */
    @DisplayName("청소년 계정으로 로그인한 후 두 역의 최소 시간 경로를 조회한다")
    @Test
    void find_path_by_duration_with_youth_account() {
        // when
        ExtractableResponse<Response> 경로_조회_결과 = 로그인_후_지하철_경로_조회_요청(청소년_회원_JADE, 교대역_ID, 양재역_ID, DURATION.name());

        // then
        조회에_성공한다(경로_조회_결과);
        역이_순서대로_정렬되어_있다(경로_조회_결과, 교대역_ID, 강남역_ID, 양재역_ID);
        총_거리와_소요_시간이_조회된다(
                경로_조회_결과,
                교대_강남_구간.구간_거리() + 강남_양재_구간.구간_거리(),
                교대_강남_구간.구간_소요시간() + 강남_양재_구간.구간_소요시간()
        );
        총_이용_요금이_조회된다(경로_조회_결과, 1600);
    }

    /**
     * Given 어린이 계정으로 로그인이 되어있고
     * When  교대역에서 양재역까지의 최소 시간 경로를 조회하면
     * Then 교대역 -> 강남역 -> 양재역 순으로 역 목록이 조회된다
     * And 어린이 요금이 할인된 지하철 총 이용 요금이 조회된다 ((1250 + 900 + 200 - 350) x 0.5 = 1000원)
     */
    @DisplayName("어린이 계정으로 로그인한 후 두 역의 최소 시간 경로를 조회한다")
    @Test
    void find_path_by_duration_with_child_account() {
        // when
        ExtractableResponse<Response> 경로_조회_결과 = 로그인_후_지하철_경로_조회_요청(어린이_회원_BEOM, 교대역_ID, 양재역_ID, DURATION.name());

        // then
        조회에_성공한다(경로_조회_결과);
        역이_순서대로_정렬되어_있다(경로_조회_결과, 교대역_ID, 강남역_ID, 양재역_ID);
        총_거리와_소요_시간이_조회된다(
                경로_조회_결과,
                교대_강남_구간.구간_거리() + 강남_양재_구간.구간_거리(),
                교대_강남_구간.구간_소요시간() + 강남_양재_구간.구간_소요시간()
        );
        총_이용_요금이_조회된다(경로_조회_결과, 1000);
    }
}
