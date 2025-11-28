#!/bin/bash

# 课程管理系统部署验证脚本
# 用法: ./verify-deployment.sh [环境: dev|prod]

set -e

# 配置
BASE_URL="http://localhost:8080"
DEFAULT_ENV="dev"
ENV=${1:-$DEFAULT_ENV}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查服务是否就绪
wait_for_service() {
    local max_attempts=30
    local attempt=1

    log_info "等待服务启动 (环境: $ENV)..."

    while [ $attempt -le $max_attempts ]; do
        if curl -s "$BASE_URL/health/info" > /dev/null 2>&1; then
            log_success "服务已启动"
            return 0
        fi

        log_info "尝试 $attempt/$max_attempts - 服务尚未就绪，等待 2 秒..."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "服务启动超时"
    return 1
}

# 健康检查
check_health() {
    log_info "执行健康检查..."

    local response
    response=$(curl -s "$BASE_URL/health/db")

    if echo "$response" | grep -q '"status":"healthy"'; then
        log_success "健康检查通过"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    else
        log_error "健康检查失败"
        echo "$response"
        return 1
    fi
}

# 数据库连接检查
check_database() {
    log_info "检查数据库连接..."

    local endpoint
    if [ "$ENV" = "dev" ]; then
        endpoint="/dev/test"
    else
        endpoint="/prod/test"
    fi

    local response
    response=$(curl -s "$BASE_URL$endpoint")

    if echo "$response" | grep -q '"status":"SUCCESS"'; then
        log_success "数据库连接正常"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    else
        log_error "数据库连接失败"
        echo "$response"
        return 1
    fi
}

# 数据验证
validate_data() {
    log_info "验证基础数据..."

    # 检查学生数据
    local students_response
    students_response=$(curl -s "$BASE_URL/api/students")
    if echo "$students_response" | grep -q '"studentId"'; then
        local student_count=$(echo "$students_response" | grep -o '"studentId"' | wc -l)
        log_success "学生数据验证通过 ($student_count 名学生)"
    else
        log_warning "学生数据验证失败或API端点不可用"
    fi

    # 检查课程数据
    local courses_response
    courses_response=$(curl -s "$BASE_URL/api/courses")
    if echo "$courses_response" | grep -q '"courseCode"'; then
        local course_count=$(echo "$courses_response" | grep -o '"courseCode"' | wc -l)
        log_success "课程数据验证通过 ($course_count 门课程)"
    else
        log_warning "课程数据验证失败或API端点不可用"
    fi
}

# 连接池监控检查
check_connection_pool() {
    log_info "检查连接池状态..."

    local response
    response=$(curl -s "$BASE_URL/monitor/pool/health")

    if echo "$response" | grep -q '"status":"HEALTHY"'; then
        log_success "连接池状态正常"
    else
        log_warning "连接池状态异常"
        echo "$response"
    fi
}

# 环境特定检查
check_environment_specific() {
    if [ "$ENV" = "dev" ]; then
        log_info "检查 H2 控制台..."
        local h2_response
        h2_response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/h2-console")
        if [ "$h2_response" = "200" ]; then
            log_success "H2 控制台可访问: $BASE_URL/h2-console"
        else
            log_warning "H2 控制台不可访问"
        fi
    else
        log_info "生产环境 MySQL 连接池检查..."
        local pool_response
        pool_response=$(curl -s "$BASE_URL/monitor/pool/status")
        if echo "$pool_response" | grep -q '"connectionPool":"HikariCP"'; then
            log_success "HikariCP 连接池配置正确"
        else
            log_warning "连接池配置异常"
        fi
    fi
}

# 生成部署报告
generate_report() {
    log_info "生成部署验证报告..."

    echo "==========================================="
    echo "       课程管理系统部署验证报告"
    echo "==========================================="
    echo "环境: $ENV"
    echo "时间: $(date)"
    echo "服务地址: $BASE_URL"
    echo ""

    if [ "$ENV" = "dev" ]; then
        echo "开发环境特性:"
        echo "  ✓ H2 内存数据库"
        echo "  ✓ H2 控制台: $BASE_URL/h2-console"
        echo "  ✓ 自动数据初始化"
    else
        echo "生产环境特性:"
        echo "  ✓ MySQL 数据库"
        echo "  ✓ HikariCP 连接池"
        echo "  ✓ 连接池监控"
    fi

    echo ""
    echo "验证端点:"
    echo "  ✓ 健康检查: $BASE_URL/health/db"
    echo "  ✓ 环境测试: $BASE_URL/{dev|prod}/test"
    echo "  ✓ 连接池监控: $BASE_URL/monitor/pool/status"
    echo ""
    echo "==========================================="
}

main() {
    log_info "开始课程管理系统部署验证 (环境: $ENV)"

    # 等待服务启动
    if ! wait_for_service; then
        log_error "部署验证失败: 服务未启动"
        exit 1
    fi

    # 执行各项检查
    check_health || exit 1
    check_database || exit 1
    validate_data
    check_connection_pool
    check_environment_specific

    # 生成报告
    generate_report

    log_success "🎉 部署验证完成！系统运行正常。"
    log_info "访问地址: $BASE_URL"
    log_info "API 文档: $BASE_URL/swagger-ui.html"

    if [ "$ENV" = "dev" ]; then
        log_info "H2 控制台: $BASE_URL/h2-console"
    fi
}

# 执行主函数
main